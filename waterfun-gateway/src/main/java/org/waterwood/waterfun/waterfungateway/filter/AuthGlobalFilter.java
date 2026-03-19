package org.waterwood.waterfun.waterfungateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.waterwood.utils.StringUtil;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;

@Component
@Order(-100)
public class AuthGlobalFilter implements GlobalFilter {

    private final PathPatternRequestMatcher.Builder pathPatternRequestMatcherBuilder;

    public AuthGlobalFilter(PathPatternRequestMatcher.Builder pathPatternRequestMatcherBuilder) {
        this.pathPatternRequestMatcherBuilder = pathPatternRequestMatcherBuilder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    Authentication auth = ctx.getAuthentication();
                    if(auth instanceof JwtAuthenticationToken jwtAuth) {
                        Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
                        Jwt jwt = jwtAuth.getToken();
                        String did = jwt.getClaimAsString("did");
                        String jti = jwt.getClaimAsString("jti");

                        ServerHttpRequest.Builder reqBuilder = exchange.getRequest().mutate();
                        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                                .header("X-User-Uid", jwt.getSubject())
                                .header("X-Token-Jti", jwt.getId())
                                .header("X-User-Roles",
                                        String.join(",",
                                                roles.stream().map(GrantedAuthority::getAuthority).toList()));


                        if(StringUtil.isNotBlank(did)){
                            requestBuilder.header("X-User-Did", did);
                        }
                        if(StringUtil.isNotBlank(jti)){
                            requestBuilder.header("X-User-Jti", jti);
                        }
                        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                    }else {
                        return chain.filter(exchange);
                    }
                });
    }
}
