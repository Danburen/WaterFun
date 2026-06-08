package org.waterwood.waterfun.waterfungateway.util;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public final class RateLimitResponseBuilder {

    private RateLimitResponseBuilder() {}

    public static Mono<Void> buildTooManyRequests(ServerWebExchange exchange, long retryAfter) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Retry-After", String.valueOf(retryAfter));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"code\":429,\"message\":\"Too many requests\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}