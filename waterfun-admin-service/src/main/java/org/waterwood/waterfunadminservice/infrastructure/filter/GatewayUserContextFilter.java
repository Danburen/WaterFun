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
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserSettingRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayUserContextFilter extends OncePerRequestFilter {

    private final UserSettingRepository userSettingRepository;

    public GatewayUserContextFilter(UserSettingRepository userSettingRepository) {
        this.userSettingRepository = userSettingRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String lang = request.getHeader("X-User-Lang");
        Locale locale = Locale.forLanguageTag(lang != null ? lang : "en");

        String userUid = request.getHeader("X-User-Uid");
        if(StringUtil.isBlank(userUid)) {
            AuthContext authCtx = new AuthContext();
            authCtx.setLocale(locale);
            authCtx.setClientIp((String) request.getAttribute("clientIp"));
            UserCtxHolder.set(authCtx);
            try {
                filterChain.doFilter(request, response);
            } finally {
                UserCtxHolder.remove();
            }
            return;
        }

        AuthContext authCtx = new AuthContext();

        String jti = request.getHeader("X-Token-Jti");
        if(StringUtil.isBlank(jti)){
            jti = request.getHeader("X-User-Jti");
        }

        authCtx.setUserUid(Long.valueOf(userUid));
        authCtx.setJti(jti);
        authCtx.setLocale(locale);
        authCtx.setDid(request.getHeader("X-User-Did"));
        authCtx.setClientIp((String) request.getAttribute("clientIp"));
        userSettingRepository.findById(authCtx.getUserUid())
                .ifPresent(authCtx::setUserSetting);

        UserCtxHolder.set(authCtx);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserCtxHolder.remove();
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
