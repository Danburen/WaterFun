package org.waterwood.waterfunservice.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class GatewayUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userUid = request.getHeader("X-User-Uid");
        if(StringUtil.isBlank(userUid)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthContext authCtx = new AuthContext();

        String jti = request.getHeader("X-Token-Jti");
        String lang = request.getHeader("X-User-Lang");
        if(StringUtil.isBlank(jti)){
            jti = request.getHeader("X-User-Jti");
        }

        authCtx.setUserUid(Long.valueOf(userUid));
        authCtx.setJti(jti);
        authCtx.setLocale(Locale.forLanguageTag(lang != null ? lang : "en"));
        authCtx.setDid(request.getHeader("X-User-Did"));
        authCtx.setClientIp((String) request.getAttribute("clientIp"));
        // TODO: ADD PERMISSIONS INJECTION
        UserCtxHolder.set(authCtx);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserCtxHolder.remove();  // must clean to prevent MEMORY_LEAK
        }
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            return Set.of();
        }
        return Set.of(rolesHeader.split(","));
    }
}
