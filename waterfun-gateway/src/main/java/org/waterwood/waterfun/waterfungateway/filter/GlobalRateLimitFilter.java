package org.waterwood.waterfun.waterfungateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.waterwood.waterfun.waterfungateway.util.CounterWindow;
import org.waterwood.waterfun.waterfungateway.util.RateLimitResponseBuilder;
import org.waterwood.waterfun.waterfungateway.util.RateLimitUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)  // 在 Auth 限流之后执行
public class GlobalRateLimitFilter implements GlobalFilter, Ordered {

    private final boolean enabled;
    private final int getLimit;
    private final int writeLimit;
    private final long windowSeconds;

    private final Cache<String, CounterWindow> counters;

    public GlobalRateLimitFilter(
            @Value("${waterfun.rate-limit.global.enabled:true}") boolean enabled,
            @Value("${waterfun.rate-limit.global.get-limit:1000}") int getLimit,
            @Value("${waterfun.rate-limit.global.write-limit:30}") int writeLimit,
            @Value("${waterfun.rate-limit.global.window-seconds:60}") long windowSeconds) {
        this.enabled = enabled;
        this.getLimit = getLimit;
        this.writeLimit = writeLimit;
        this.windowSeconds = windowSeconds;

        long safeWindowSeconds = Math.max(1, windowSeconds);
        long ttlSeconds = Math.max(120, safeWindowSeconds * 2);
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(50_000)  // more capacity for more ips in general
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        if (!enabled || RateLimitUtils.isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String ip = RateLimitUtils.getClientIp(request);
        String key = ip + ":" + method;  // aggregate with ip and method
        int limit = RateLimitUtils.isWriteMethod(method) ? writeLimit : getLimit;

        long nowEpochSecond = Instant.now().getEpochSecond();
        CounterWindow counter = Objects.requireNonNull(
                counters.get(key, k -> new CounterWindow(nowEpochSecond))
        );

        int currentCount;
        long retryAfter;
        long safeWindow = Math.max(1, windowSeconds);

        synchronized (counter) {
            if (nowEpochSecond - counter.getWindowStartEpochSecond() >= safeWindow) {
                counter.setWindowStartEpochSecond(nowEpochSecond);
                counter.getCounter().set(0);
            }
            currentCount = counter.getCounter().incrementAndGet();
            retryAfter = Math.max(1, safeWindow - (nowEpochSecond - counter.getWindowStartEpochSecond()));
        }

        if (currentCount > limit) {
            return RateLimitResponseBuilder.buildTooManyRequests(exchange, retryAfter);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}