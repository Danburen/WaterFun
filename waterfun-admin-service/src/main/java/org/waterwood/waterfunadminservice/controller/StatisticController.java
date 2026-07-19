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
import org.waterwood.waterfunadminservice.api.response.SiteStatisticResponse;
import org.waterwood.waterfunadminservice.service.StatisticService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticController {
    private final StatisticService statisticService;

    @GetMapping("/list")
    public ApiResponse<Page<SiteStatisticResponse>> listStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable = PageRequest.of(Math.max(0, pageable.getPageNumber() - 1), pageable.getPageSize(), pageable.getSort());
        return ApiResponse.success(statisticService.listStatistics(startDate, endDate, pageable));
    }

    @GetMapping("/latest")
    public ApiResponse<SiteStatisticResponse> getLatestStatistic() {
        return ApiResponse.success(statisticService.getLatestStatistic());
    }

    @GetMapping("/{date}")
    public ApiResponse<SiteStatisticResponse> getStatistic(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(statisticService.getStatistic(date));
    }
}
