package org.waterwood.waterfunservice.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.security.IpAccessLogRecorder;

import java.io.IOException;

/**
 * Logs every user-facing request's IP access to ip_access_log table asynchronously.
 * Delegates to IpAccessLogRecorder for async persistence + geo lookup.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class IpAccessLogFilter extends OncePerRequestFilter {

    private final IpAccessLogRecorder ipAccessLogRecorder;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null
                || path.contains("/actuator")
                || path.contains("/swagger")
                || path.contains("/v3/api-docs")
                || path.contains("/favicon")
                || path.contains("/webjars");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Capture values before delegating — UserCtxHolder is ThreadLocal and cleared after chain
        String clientIp = resolveClientIp(request);
        Long userUid = UserCtxHolder.getUserUid();

        filterChain.doFilter(request, response);

        if (StringUtil.isBlank(clientIp)) return;

        ipAccessLogRecorder.record(
                clientIp, userUid,
                request.getRequestURI(), request.getMethod(),
                (short) response.getStatus()
        );
    }

    static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-Client-Ip");
        if (StringUtil.isNotBlank(ip)) return ip;

        ip = request.getHeader("X-Forwarded-For");
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }
}
