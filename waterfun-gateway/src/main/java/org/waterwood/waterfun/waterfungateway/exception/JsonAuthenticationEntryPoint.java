package org.waterwood.waterfun.waterfungateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.ErrorResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ErrorResponse body = new ErrorResponse(
                BaseResponseCode.INVALID_TOKEN_OR_EXPIRED.getCode(),
                ex.getMessage(),
                null,
                new Date()
        );
        return writeJson(exchange, HttpStatus.UNAUTHORIZED, body);
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

