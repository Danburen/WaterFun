package org.waterwood.waterfunservice.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;

import java.io.IOException;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class ALayerPageViewFilter extends OncePerRequestFilter {

    private final SiteStatisticRecorder siteStatisticRecorder;

    @Value("${stat.exclude-paths:/api/monitor/**}")
    private List<String> excludePaths;

    @Value("${stat.exclude-internal-ips:true}")
    private boolean excludeInternalIps;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if ("GET".equals(request.getMethod()) && response.getStatus() < 400) {
            if (!isExcluded(request)) {
                siteStatisticRecorder.recordPageView();
            }
        }
    }

    private boolean isExcluded(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : excludePaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        if (excludeInternalIps) {
            String clientIp = (String) request.getAttribute("clientIp");
            if (clientIp != null && isInternalIp(clientIp)) {
                return true;
            }
        }

        return false;
    }

    private boolean isInternalIp(String ip) {
        if (ip == null) return false;
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return true;
        if (ip.startsWith("127.") || ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
