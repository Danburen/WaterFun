package org.waterwood.waterfun.waterfungateway.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.waterwood.waterfun.waterfungateway.util.CounterWindow;
import org.waterwood.waterfun.waterfungateway.util.RateLimitResponseBuilder;
import org.waterwood.waterfun.waterfungateway.util.RateLimitUtils;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter implements GlobalFilter, Ordered {

    private final boolean enabled;
    private final int requestLimit;
    private final long windowSeconds;
    private final List<String> pathPatterns;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Cache<String, CounterWindow> counters;

    public AuthRateLimitFilter(
            @Value("${waterfun.rate-limit.auth.enabled:true}") boolean enabled,
            @Value("${waterfun.rate-limit.auth.requests:20}") int requestLimit,
            @Value("${waterfun.rate-limit.auth.window-seconds:60}") long windowSeconds,
            @Value("${waterfun.rate-limit.auth.paths:/api/auth/login,/api/auth/register,/api/auth/refresh}") String pathsCsv) {
        this.enabled = enabled;
        this.requestLimit = requestLimit;
        this.windowSeconds = windowSeconds;
        this.pathPatterns = Arrays.stream(pathsCsv.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();

        long safeWindowSeconds = Math.max(1, windowSeconds);
        long ttlSeconds = Math.max(120, safeWindowSeconds * 2);
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(10_000)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (!enabled || !isLimitedPath(path)) {
            return chain.filter(exchange);
        }

        String key = RateLimitUtils.getClientIp(request) + ":" + path;
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

        if (currentCount > requestLimit) {
            return RateLimitResponseBuilder.buildTooManyRequests(exchange, retryAfter);
        }

        return chain.filter(exchange);
    }

    private boolean isLimitedPath(String path) {
        return pathPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }


}
