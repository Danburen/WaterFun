package org.waterwood.waterfunservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.waterwood.waterfunservice.infrastructure.filter.GatewayUserContextFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GatewayUserContextFilter gatewayUserContextFilter;

    public SecurityConfig(GatewayUserContextFilter gatewayUserContextFilter) {
        this.gatewayUserContextFilter = gatewayUserContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF as Gateway handles auth
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/api/public/**", "/error").permitAll() // Allow public endpoints
                        .anyRequest().permitAll() // Allow all internal requests (Trust Gateway for now)
                )
                // Add the custom filter to populate UserCtxHolder
                .addFilterBefore(gatewayUserContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
