package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.response.OnlineUserStatsVO;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.util.List;

@RestController
@RequestMapping("/api/online-users")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;
    private final SiteStatisticRecorder siteStatisticRecorder;
    private final UserBriefService userBriefService;

    @GetMapping("/stats")
    public ApiResponse<OnlineUserStatsVO> getStats() {
        long onlineCount = onlineUserService.getOnlineCount();
        long todayNewUsers = siteStatisticRecorder.getTodayNewUsers();
        long todayPeakOnline = siteStatisticRecorder.getTodayPeakOnline();
        return ApiResponse.success(new OnlineUserStatsVO(onlineCount, todayNewUsers, todayPeakOnline));
    }

    @GetMapping("/list")
    public ApiResponse<Page<UserBrief>> listOnlineUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Long> uidPage = onlineUserService.listOnlineUserIdsPage(Math.max(page - 1, 0), size);
        List<UserBrief> briefs = userBriefService.listUseBriefs(uidPage.getContent());
        return ApiResponse.success(new org.springframework.data.domain.PageImpl<>(
                briefs, uidPage.getPageable(), uidPage.getTotalElements()));
    }
}
