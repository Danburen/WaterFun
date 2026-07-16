package org.waterwood.waterfunadminservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.OnlineCountVO;
import org.waterwood.waterfunadminservice.api.response.OnlineUserVO;
import org.waterwood.waterfunadminservice.service.AdminOnlineService;
import org.waterwood.waterfunadminservice.service.StatisticService;
import org.waterwood.waterfunservicecore.entity.user.UserType;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/online-users")
@RequiredArgsConstructor
public class OnlineUserController {

    private final AdminOnlineService adminOnlineService;
    private final StatisticService statisticService;

    @GetMapping
    public ApiResponse<Page<OnlineUserVO>> listOnlineUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) Short levelMin,
            @RequestParam(required = false) Short levelMax) {
        return ApiResponse.success(adminOnlineService.listOnlineUsers(page, size, keyword, userType, levelMin, levelMax));
    }

    @GetMapping("/count")
    public ApiResponse<OnlineCountVO> getOnlineCount() {
        long total = adminOnlineService.getOnlineCount();
        long admin = adminOnlineService.getAdminOnlineCount();
        long peakOnlineCount = statisticService.getStatistic(LocalDate.now()).getPeakOnline();
        if (peakOnlineCount > total) {
            peakOnlineCount = total;
        }
        return ApiResponse.success(new OnlineCountVO(total, admin, peakOnlineCount));
    }

    @PostMapping("/{uid}/force-offline")
    public ApiResponse<Void> forceOffline(@PathVariable Long uid) {
        adminOnlineService.forceOffline(uid);
        return ApiResponse.success();
    }
}
