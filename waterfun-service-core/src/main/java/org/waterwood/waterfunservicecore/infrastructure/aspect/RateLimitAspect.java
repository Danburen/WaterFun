package org.waterwood.waterfunservicecore.infrastructure.aspect;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.waterwood.common.exceptions.RateLimitException;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import javax.swing.*;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {
    private final Cache<String, WindowCounter> cache;

    public RateLimitAspect() {
        this.cache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = generateKey(joinPoint, rateLimit.key());
        WindowCounter counter = cache.get(key, k -> new WindowCounter());

        if(Objects.requireNonNull(counter).allow(rateLimit.permits(), rateLimit.window())){
            return joinPoint.proceed();
        } else {
            throw new RateLimitException();
        }
    }

    private String generateKey(ProceedingJoinPoint joinPoint, String keyType) {
        String method = joinPoint.getTarget().getClass().getSimpleName()
                + "." + joinPoint.getSignature().getName();

        String dimension = switch (keyType) {
            case "user" -> getCurrentUserId();
            case "ip" -> getCurrentIp();
            case "method" -> "global";
            default -> keyType;
        };

        return method + ":" + dimension;
    }

    private String getCurrentUserId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            String userId = servletAttrs.getRequest().getHeader("X-User-Id");
            if (userId != null) return userId;
        }
        return "anon";
    }

    private String getCurrentIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest().getRemoteAddr();
        }
        return "unknown";
    }

    private static class WindowCounter {
        private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();

        synchronized boolean allow(int permits, int windowSeconds) {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSeconds * 1000L;
            // clear expiration
            timestamps.removeIf(t -> t < windowStart);
            // check and result
            if (timestamps.size() >= permits) {
                return false;
            }
            timestamps.offer(now);
            return true;
        }
    }
}
