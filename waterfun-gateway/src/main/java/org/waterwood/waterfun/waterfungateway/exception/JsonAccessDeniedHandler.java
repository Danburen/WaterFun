package org.waterwood.waterfun.waterfungateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.ErrorResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        ErrorResponse body = new ErrorResponse(
                BaseResponseCode.HTTP_FORBIDDEN.getCode(),
                denied.getMessage(),
                null,
                new Date()
        );
        return writeJson(exchange, HttpStatus.FORBIDDEN, body);
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, ErrorResponse body) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(body))
                .flatMap(bytes -> exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
                ))
                .onErrorResume(writeErr -> {
                    String fallback = "{\"code\":\"" + body.getCode()
                            + "\",\"message\":\"" + body.getMessage()
                            + "\",\"timestamp\":" + new Date().getTime() + "}";
                    byte[] bytes = fallback.getBytes(StandardCharsets.UTF_8);
                    return exchange.getResponse().writeWith(
                            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
                    );
                });
    }
}

