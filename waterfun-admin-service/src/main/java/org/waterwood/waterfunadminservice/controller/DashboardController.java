package org.waterwood.waterfunadminservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.*;
import org.waterwood.waterfunadminservice.service.AdminOnlineService;
import org.waterwood.waterfunadminservice.service.DashboardService;
import org.waterwood.waterfunadminservice.service.StatisticService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticService statisticService;
    private final AdminOnlineService adminOnlineService;
    private final DashboardService dashboardService;

    @GetMapping("/statistics/list")
    public ApiResponse<Page<SiteStatisticResponse>> listStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable = PageRequest.of(Math.max(0, pageable.getPageNumber() - 1), pageable.getPageSize(), pageable.getSort());
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminOnlineService.listOnlineUsers(Math.max(page - 1, 0), size, null, null, null, null));
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview() {
        return ApiResponse.success(dashboardService.getOverview());
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
