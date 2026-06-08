package org.waterwood.waterfun.waterfungateway.filter;

import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfun.waterfungateway.util.RateLimitUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

@Component
@Order(10)
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private String normalizeLanguage(String acceptLang) {
        if (StringUtil.isBlank(acceptLang)) {
            return "en";
        }
        String first = acceptLang.split(",")[0].trim();
        String lang = first.split("-")[0].toLowerCase();
        return Set.of("zh", "en", "ja", "ko").contains(lang) ? lang : "en";
    }

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange,@NonNull GatewayFilterChain chain) {
        String acceptLang = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCEPT_LANGUAGE);
        String normalizedLang = normalizeLanguage(acceptLang);
        String clientIp = RateLimitUtils.getClientIp(exchange.getRequest());
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    String did = jwt.getClaimAsString("did");
                    String jti = jwt.getClaimAsString("jti");

                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                            .header("X-User-Uid", jwt.getSubject())
                            .header("X-User-Lang", normalizedLang);

                    if (StringUtil.isNotBlank(did)) {
                        requestBuilder.header("X-User-Did", did);
                    }
                    if (StringUtil.isNotBlank(jti)) {
                        requestBuilder.header("X-User-Jti", jti);
                        requestBuilder.header("X-Token-Jti", jti); // Consistency
                    }

                    return exchange.mutate().request(requestBuilder.build()).build();
                })
                .defaultIfEmpty(
                        exchange.mutate().request(
                                exchange.getRequest().mutate()
                                        .header("X-User-Lang", normalizedLang)
                                        .header("X-Real-Client-Ip", clientIp)
                                        .build()
                        ).build()
                )
                .flatMap(chain::filter);
    }
    @Override
    public int getOrder() {
        return 10;
    }
}
