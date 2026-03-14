package org.waterwood.waterfun.waterfungateway.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int requestLimit;
    private final long windowSeconds;
    private final List<String> pathPatterns;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, CounterWindow> counters = new ConcurrentHashMap<>();

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
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || !isLimitedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildKey(request);
        long nowEpochSecond = Instant.now().getEpochSecond();

        CounterWindow counter = counters.computeIfAbsent(key, ignored -> new CounterWindow(nowEpochSecond));

        int currentCount;
        long retryAfter;

        synchronized (counter) {
            if ((nowEpochSecond - counter.windowStartEpochSecond) >= windowSeconds) {
                counter.windowStartEpochSecond = nowEpochSecond;
                counter.counter.set(0);
            }

            currentCount = counter.counter.incrementAndGet();
            retryAfter = Math.max(1, windowSeconds - (nowEpochSecond - counter.windowStartEpochSecond));
        }

        if (currentCount > requestLimit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLimitedPath(String path) {
        return pathPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String buildKey(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String path = request.getRequestURI();
        return remoteAddr + ":" + path;
    }

    private static class CounterWindow {
        private final AtomicInteger counter = new AtomicInteger(0);
        private long windowStartEpochSecond;

        private CounterWindow(long windowStartEpochSecond) {
            this.windowStartEpochSecond = windowStartEpochSecond;
        }
    }
}

