package org.waterwood.waterfunadminservice.infrastructure.filter;

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
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userUid = request.getHeader("X-User-Uid");
        if(StringUtil.isBlank(userUid)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthContext authContext = new AuthContext();
        authContext.setUserUid(Long.valueOf(userUid));
        authContext.setRoles(parseRoles(request.getHeader("X-User-Roles")));

        String jti = request.getHeader("X-Token-Jti");
        if(StringUtil.isBlank(jti)){
            jti = request.getHeader("X-User-Jti");
        }

        authContext.setJti(jti);
        authContext.setDid(request.getHeader("X-User-Did"));
        // TODO: ADD PERMISSIONS INJECTION
        UserCtxHolder.set(authContext);
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
