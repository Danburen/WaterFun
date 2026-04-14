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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

        String jti = request.getHeader("X-Token-Jti");
        if(StringUtil.isBlank(jti)){
            jti = request.getHeader("X-User-Jti");
        }

        authContext.setJti(jti);
        authContext.setDid(request.getHeader("X-User-Did"));
        UserCtxHolder.set(authContext);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserCtxHolder.remove();  // must clean to prevent MEMORY_LEAK
        }
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (StringUtil.isBlank(rolesHeader)) {
            return new HashSet<>();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> parsePermissions(String permsHeader) {
        if (StringUtil.isBlank(permsHeader)) {
            return new HashSet<>();
        }
        return Arrays.stream(permsHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
