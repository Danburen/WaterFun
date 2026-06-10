package org.waterwood.waterfunadminservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.DashboardOverviewVO;
import org.waterwood.waterfunadminservice.api.response.DashboardRecentActivityVO;
import org.waterwood.waterfunadminservice.api.response.OnlineUserVO;
import org.waterwood.waterfunadminservice.api.response.SiteStatisticResponse;
import org.waterwood.waterfunadminservice.api.response.TrendPointVO;
import org.waterwood.waterfunadminservice.service.AdminOnlineService;
import org.waterwood.waterfunadminservice.service.DashboardService;
import org.waterwood.waterfunadminservice.service.StatisticService;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.SiteStatisticRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticService statisticService;
    private final AdminOnlineService adminOnlineService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final AuditTaskRepository auditTaskRepository;
    private final SiteStatisticRepository siteStatisticRepository;

    @GetMapping("/statistics/list")
    public ApiResponse<Page<SiteStatisticResponse>> listStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(statisticService.listStatistics(startDate, endDate, pageable));
    }

    @GetMapping("/statistics/latest")
    public ApiResponse<SiteStatisticResponse> getLatestStatistic() {
        return ApiResponse.success(statisticService.getLatestStatistic());
    }

    @GetMapping("/statistics/{date}")
    public ApiResponse<SiteStatisticResponse> getStatistic(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(statisticService.getStatistic(date));
    }

    @GetMapping("/online-users")
    public ApiResponse<Page<OnlineUserVO>> listOnlineUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminOnlineService.listOnlineUsers(page, size, null, null, null, null));
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview() {
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long pendingModerations = auditTaskRepository.countByStatus(AuditStatus.PENDING);
        var todayStat = siteStatisticRepository.findById(LocalDate.now());
        long todayNewUsers = todayStat.map(s -> s.getNewUsers() != null ? s.getNewUsers() : 0L).orElse(0L);
        long todayNewPosts = todayStat.map(s -> s.getNewPosts() != null ? s.getNewPosts() : 0L).orElse(0L);
        long todayPv = todayStat.map(s -> s.getDailyPv() != null ? s.getDailyPv() : 0L).orElse(0L);
        long onlineUserCount = adminOnlineService.getOnlineCount();
        long peakOnline = todayStat.map(s -> s.getPeakOnline() != null ? s.getPeakOnline() : 0L).orElse(0L);
        return ApiResponse.success(new DashboardOverviewVO(
                onlineUserCount, totalUsers, totalPosts, todayNewUsers, todayNewPosts, todayPv, pendingModerations, peakOnline
        ));
    }

    @GetMapping("/recent-activities")
    public ApiResponse<List<DashboardRecentActivityVO>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(dashboardService.getRecentActivities(limit));
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendPointVO>> getTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(dashboardService.getTrend(days));
    }
}
