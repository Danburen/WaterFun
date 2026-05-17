package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservice.api.response.SystemNotificationRes;
import org.waterwood.waterfunservice.service.NotificationService;
import org.waterwood.waterfunservicecore.api.CursorPage;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    // System Notifications
    @GetMapping("/system/list")
    public ApiResponse<CursorPage<SystemNotificationRes, Long>> list(@RequestParam(required = false) Long cursor,
                                                  @RequestParam(defaultValue = "10") Integer limit,
                                                  @RequestParam(required = false) Boolean unreadOnly) {
        return ApiResponse.success(
                notificationService.list(cursor, limit, unreadOnly)
        );
    }

    @GetMapping("/system/unreadCount")
    public ApiResponse<Integer> unreadCount() {
        return ApiResponse.success(notificationService.systemUnreadCount());
    }

    @PostMapping("/system/{id}/batchMarkRead")
    public ApiResponse<Void> batchMarkRead(@PathVariable Long id) {
        notificationService.markSystemNotificationRead(id);
        return ApiResponse.success();
    }

    @PostMapping("/system/markAllRead")
    public ApiResponse<Void> markAllRead() {
        notificationService.markSystemNotificationAllRead();
        return ApiResponse.success();
    }

    @PostMapping("/system/markRead")
    public ApiResponse<BatchResult> batchMarkRead(@RequestBody BatchMarkReadReq req) {
        return ApiResponse.success(
                notificationService.batchMarkSystemNotificationRead(req)
        );
    }
}
