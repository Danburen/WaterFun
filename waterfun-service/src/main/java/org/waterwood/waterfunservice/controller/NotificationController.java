package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservice.api.response.notifications.UnreadCountResp;
import org.waterwood.waterfunservice.service.NotificationService;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.entity.notification.NoticeGroup;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/list")
    public ApiResponse<CursorPage<InboxNotificationRes, Long>> list(@RequestParam(required = false) Long cursor,
                                                                    @RequestParam(defaultValue = "10") Integer limit,
                                                                    @RequestParam(required = false) Boolean unreadOnly,
                                                                    @RequestParam(required = false) NoticeType type,
                                                                    @RequestParam(required = false) NoticeGroup group) {
        List<NoticeType> types = null;
        if (type != null) {
            types = List.of(type);
        } else if (group != null) {
            types = Arrays.stream(NoticeType.values())
                    .filter(t -> t.getGroup() == group)
                    .toList();
        }
        return ApiResponse.success(
                notificationService.list(cursor, limit, unreadOnly, types)
        );
    }

    @GetMapping("/unreadCount")
    public ApiResponse<UnreadCountResp> unreadCount() {
        return ApiResponse.success(notificationService.getUnreadCountWithTabs());
    }

    @PostMapping("/read/{id}")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markInboxRead(id);
        return ApiResponse.success();
    }

    @PostMapping("/markAllRead")
    public ApiResponse<Void> markAllRead() {
        notificationService.markSystemNotificationAllRead();
        return ApiResponse.success();
    }

    @PostMapping("/batchMarkRead")
    public ApiResponse<BatchResult> batchMarkRead(@RequestBody BatchMarkReadReq req) {
        return ApiResponse.success(
                notificationService.batchMarkSystemNotificationRead(req)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationService.deleteInbox(id);
        return ApiResponse.success();
    }
}
