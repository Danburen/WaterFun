package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.waterwood.waterfunservice.service.SSEService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class SSEController {

    private final SSEService sseService;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long uid = UserCtxHolder.getUserUid();
        return sseService.subscribe(uid);
    }
}
