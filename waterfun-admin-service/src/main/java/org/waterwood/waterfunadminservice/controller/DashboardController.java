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
import org.waterwood.waterfunadminservice.api.response.OnlineUserVO;
import org.waterwood.waterfunadminservice.api.response.SiteStatisticResponse;
import org.waterwood.waterfunadminservice.service.OnlineUserAdminService;
import org.waterwood.waterfunadminservice.service.StatisticService;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.SiteStatisticRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticService statisticService;
    private final OnlineUserAdminService onlineUserAdminService;
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
        return ApiResponse.success(onlineUserAdminService.listOnlineUsers(page, size));
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview() {
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long pendingModerations = auditTaskRepository.countByStatus(AuditStatus.PENDING);
        long todayNewUsers = siteStatisticRepository.findById(LocalDate.now())
                .map(s -> s.getNewUsers() != null ? s.getNewUsers() : 0L)
                .orElse(0L);
        long todayNewPosts = siteStatisticRepository.findById(LocalDate.now())
                .map(s -> s.getNewPosts() != null ? s.getNewPosts() : 0L)
                .orElse(0L);
        long onlineUserCount = onlineUserAdminService.getOnlineCount();
        return ApiResponse.success(new DashboardOverviewVO(
                onlineUserCount, totalUsers, totalPosts, todayNewUsers, todayNewPosts, pendingModerations
        ));
    }
}
