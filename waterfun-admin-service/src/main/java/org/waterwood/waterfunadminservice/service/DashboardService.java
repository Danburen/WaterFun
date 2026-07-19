package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunadminservice.api.response.DashboardOverviewVO;
import org.waterwood.waterfunadminservice.api.response.DashboardRecentActivityVO;
import org.waterwood.waterfunadminservice.api.response.TrendPointVO;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.SiteStatistic;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.entity.audit.UserActivityLog;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.SiteStatisticRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.audit.UserActivityLogService;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final OnlineUserService onlineUserService;
    private final UserActivityLogService userActivityLogService;
    private final UserBriefService userBriefService;
    private final SiteStatisticRepository siteStatisticRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final AuditTaskRepository auditTaskRepository;

    public List<DashboardRecentActivityVO> getRecentActivities(int limit) {
        Set<String> onlineIds = onlineUserService.getOnlineUserIds();
        Set<Long> onlineUidSet = onlineIds != null
                ? onlineIds.stream().map(Long::valueOf).collect(Collectors.toSet())
                : Collections.emptySet();

        List<UserActivityLog> activities = userActivityLogService.findRecentActivities(limit * 2);

        Map<Long, List<UserActivityLog>> grouped = activities.stream()
                .collect(Collectors.groupingBy(UserActivityLog::getUserId));

        List<Long> userIds = new ArrayList<>(grouped.keySet());
        Map<Long, UserBrief> briefMap = userBriefService.queryForMapUserIdBriefMap(userIds);

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long uid = entry.getKey();
                    UserActivityLog latest = entry.getValue().stream()
                            .max(Comparator.comparing(UserActivityLog::getCreatedAt))
                            .orElse(null);
                    if (latest == null) return null;
                    Long lastActive = onlineUserService.getLastActive(uid);
                    return new DashboardRecentActivityVO(
                            briefMap.get(uid),
                            lastActive,
                            onlineUidSet.contains(uid),
                            latest.getActionType(),
                            latest.getBusinessType(),
                            latest.getCreatedAt(),
                            latest.getTargetId(),
                            buildDescription(latest, briefMap.get(uid))
                    );
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    if (a.getActionTime() == null && b.getActionTime() == null) return 0;
                    if (a.getActionTime() == null) return 1;
                    if (b.getActionTime() == null) return -1;
                    return b.getActionTime().compareTo(a.getActionTime());
                })
                .limit(limit)
                .toList();
    }

    public List<TrendPointVO> getTrend(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1);
        List<SiteStatistic> stats = siteStatisticRepository.findFromDate(start);
        Set<LocalDate> existing = stats.stream().map(SiteStatistic::getId).collect(Collectors.toSet());

        List<TrendPointVO> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = start.plusDays(i);
            if (existing.contains(date)) {
                SiteStatistic s = stats.stream().filter(st -> st.getId().equals(date)).findFirst().get();
                result.add(new TrendPointVO(date,
                        s.getDailyPv() != null ? s.getDailyPv() : 0L,
                        s.getNewUsers() != null ? s.getNewUsers() : 0L,
                        s.getNewPosts() != null ? s.getNewPosts() : 0L));
            } else {
                result.add(new TrendPointVO(date, 0L, 0L, 0L));
            }
        }
        return result;
    }

    public DashboardOverviewVO getOverview() {
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long pendingModerations = auditTaskRepository.countByStatus(AuditStatus.PENDING);
        var todayStat = siteStatisticRepository.findById(LocalDate.now());
        long todayNewUsers = todayStat.map(s -> s.getNewUsers() != null ? s.getNewUsers() : 0L).orElse(0L);
        long todayNewPosts = todayStat.map(s -> s.getNewPosts() != null ? s.getNewPosts() : 0L).orElse(0L);
        long todayPv = todayStat.map(s -> s.getDailyPv() != null ? s.getDailyPv() : 0L).orElse(0L);
        long onlineUserCount = onlineUserService.getOnlineCount();
        long peakOnline = todayStat.map(s -> s.getPeakOnline() != null ? s.getPeakOnline() : 0L).orElse(0L);
        return new DashboardOverviewVO(
                onlineUserCount, totalUsers, totalPosts, todayNewUsers, todayNewPosts, todayPv, pendingModerations, peakOnline
        );
    }

    private String buildDescription(UserActivityLog log, UserBrief brief) {
        String name = brief != null ? brief.getDisplayName() : "用户";
        UserActionType action = log.getActionType();
        BusinessType biz = log.getBusinessType();
        String target = log.getTargetId() != null ? String.valueOf(log.getTargetId()) : "";

        if (action == UserActionType.CREATE && biz == BusinessType.POST) {
            return name + " 发布了新帖子" + (target.isEmpty() ? "" : " #" + target);
        }
        if (action == UserActionType.DELETED && biz == BusinessType.POST) {
            return name + " 删除了帖子" + (target.isEmpty() ? "" : " #" + target);
        }
        if (action == UserActionType.INTERACTIVE && biz == BusinessType.POST) {
            return name + " 点赞了帖子" + (target.isEmpty() ? "" : " #" + target);
        }
        if (action == UserActionType.CREATE && biz == BusinessType.COMMENT) {
            return name + " 发表了评论" + (target.isEmpty() ? "" : " #" + target);
        }
        if (action == UserActionType.DELETED && biz == BusinessType.COMMENT) {
            return name + " 删除了评论" + (target.isEmpty() ? "" : " #" + target);
        }
        if (action == UserActionType.INTERACTIVE && biz == BusinessType.COMMENT) {
            return name + " 点赞了评论" + (target.isEmpty() ? "" : " #" + target);
        }
        if (biz == BusinessType.USER) {
            if (action == UserActionType.CREATE) return name + " 注册成为新用户";
            if (action == UserActionType.UPDATED) return name + " 更新了个人资料";
        }
        if (action == UserActionType.REPORT) return name + " 提交了举报";
        return name + " 进行了操作";
    }
}
