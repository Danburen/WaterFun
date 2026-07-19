package org.waterwood.waterfunservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.waterwood.waterfunservice.service.SSEService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "SSE 实时推送", description = "Server-Sent Events 长连接，用于实时推送通知到客户端")
public class SSEController {

    private final SSEService sseService;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "订阅 SSE 实时通知流",
            description = """
                    建立 SSE 长连接，登录用户通过此接口订阅实时通知推送。
                    连接建立后服务端会定时发送心跳维持连接，有通知时推送 notification 事件。

                    ## 事件类型
                    | 事件名 | 说明 | 频率 |
                    |--------|------|------|
                    | `heartbeat` | 心跳保活，无数据 | 每 30s |
                    | `notification` | 实时通知推送 | 有通知时触发 |

                    ## 前端使用示例 (JavaScript)
                    ```js
                    const sse = new EventSource('/api/notifications/sse', { withCredentials: true });

                    sse.addEventListener('notification', (event) => {
                      const data = JSON.parse(event.data);
                      console.log('新通知:', data);
                    });

                    sse.addEventListener('heartbeat', () => {});
                    ```

                    ## 注意事项
                    - 需要登录态（请求自动携带认证 Cookie/Token）
                    - 断开后浏览器 EventSource 会自动重连
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SSE 事件流，持续推送事件",
                            content = @Content(
                                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                    schema = @Schema(implementation = SseEmitter.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "心跳事件",
                                                    value = "event: heartbeat\ndata: \n\n"
                                            ),
                                            @ExampleObject(
                                                    name = "通知事件",
                                                    value = """
                                                            event: notification
                                                            data: {"id":1,"title":"新回复","noticeType":1,"content":{"text":"有人回复了你的帖子","refId":123},"createdAt":"2026-06-10T08:00:00Z","isRead":false}
                                                            """
                                            )
                                    }
                            ),
                            headers = {
                                    @Header(name = "Content-Type", description = "text/event-stream"),
                                    @Header(name = "Cache-Control", description = "no-cache"),
                                    @Header(name = "X-Accel-Buffering", description = "no")
                            }
                    ),
                    @ApiResponse(responseCode = "401", description = "未登录，无法建立 SSE 连接")
            }
    )
    public SseEmitter subscribe() {
        Long uid = UserCtxHolder.getUserUid();
        String ip = UserCtxHolder.getClientIp();
//        log.info("User {} with IP {} is subscribing to SSE", uid, ip);
        return sseService.subscribe(uid, ip);
    }
}
