package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.response.OnlineUserStatsVO;
import org.waterwood.waterfunservicecore.entity.SiteStatistic;
import org.waterwood.waterfunservicecore.infrastructure.persistence.SiteStatisticRepository;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/online-users")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;
    private final SiteStatisticRepository siteStatisticRepository;
    private final SiteStatisticRecorder siteStatisticRecorder;

    @GetMapping("/stats")
    public ApiResponse<OnlineUserStatsVO> getStats() {
        long onlineCount = onlineUserService.getOnlineCount();
        SiteStatistic todayStat = siteStatisticRepository.findById(LocalDate.now()).orElse(null);
        long todayNewUsers = (todayStat != null ? todayStat.getNewUsers() : 0L)
                + siteStatisticRecorder.getCachedNewUsers();
        long todayPeakOnline = todayStat != null && todayStat.getPeakOnline() != null
                ? todayStat.getPeakOnline() : 0L;
        todayPeakOnline = Math.max(todayPeakOnline, siteStatisticRecorder.getCachedPeakOnline());
        return ApiResponse.success(new OnlineUserStatsVO(onlineCount, todayNewUsers, todayPeakOnline));
    }
}
