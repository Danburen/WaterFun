package org.waterwood.waterfun.waterfungateway.util;


import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;
import java.util.Set;

public final class RateLimitUtils {

    private RateLimitUtils() {}

    // white list
    private static final List<String> WHITE_LIST = List.of(
//            "/health",
//            "/actuator",
//            "/favicon.ico",
//            "/swagger-ui",
//            "/v3/api-docs",
//            "/webjars"
    );

    // Set of written method
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    /**
     * Whether is written method
     */
    public static boolean isWriteMethod(String method) {
        return method != null && WRITE_METHODS.contains(method.toUpperCase());
    }

    /**
     * Detect whether the request path is in the white list (bypass rate limit)
     */
    public static boolean isWhiteListed(String path) {
        if (path == null) return false;
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * Obtain the client's real IP (bypass reverse proxy)
     */
    public static String getClientIp(ServerHttpRequest request) {
        // X-Forwarded-For: client, proxy1, proxy2
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (hasValidIp(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeaders().getFirst("X-Real-IP");
        if (hasValidIp(ip)) {
            return ip.trim();
        }

        if (request.getRemoteAddress() != null
                && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private static boolean hasValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}