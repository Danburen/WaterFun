package org.waterwood.waterfun.waterfungateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.ErrorResponse;
import org.waterwood.common.exceptions.AuthException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Order(-2)
@RequiredArgsConstructor
public class AuthErrorWebExceptionHandler implements WebExceptionHandler {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        if (!(ex instanceof AuthenticationException
                || ex instanceof JwtException
                || ex instanceof ExpiredJwtException
                || ex instanceof AuthException)) {
            return Mono.error(ex);
        } // non-auth exceptions will be handled by other handlers, here we just pass.
        ErrorResponse body;
        if(ex instanceof AuthException authException) {
            body = new ErrorResponse(
                    authException.getErrorCode(),
                    ex.getMessage(),
                    null,
                    new Date()
            );
        } else {
            body = new ErrorResponse(
                    BaseResponseCode.INVALID_TOKEN_OR_EXPIRED.getCode(),
                    "Authentication failed",
                    null,
                    new Date()
            );
        }
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
