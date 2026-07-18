package org.waterwood.waterfunadminservice.confirguation;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.waterwood.waterfunadminservice.infrastructure.filter.GatewayUserContextFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GatewayUserContextFilter gatewayUserContextFilter;

    /**
     * 可信网关 IP 白名单（逗号分隔）。
     * 生产环境应设置为网关的内网 IP，禁止外部直接访问下游服务端口。
     * 空值 = 允许所有 IP（开发模式兼容）。
     */
    @Value("${gateway.trusted-ips:}")
    private String trustedIps;

    public SecurityConfig(GatewayUserContextFilter gatewayUserContextFilter) {
        this.gatewayUserContextFilter = gatewayUserContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/auth/**", "/error").permitAll()
                        .anyRequest().access(gatewayIpAuthorizationManager())
                )
                .addFilterBefore(gatewayUserContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> gatewayIpAuthorizationManager() {
        return (authentication, context) -> {
            if (trustedIps == null || trustedIps.isBlank()) {
                return new AuthorizationDecision(true); // 开发模式：放行所有
            }
            HttpServletRequest request = context.getRequest();
            String remoteAddr = request.getRemoteAddr();
            boolean allowed = Arrays.stream(trustedIps.split(","))
                    .map(String::trim)
                    .filter(ip -> !ip.isEmpty())
                    .anyMatch(ip -> ip.equals(remoteAddr));
            return new AuthorizationDecision(allowed);
        };
    }
}
