package org.waterwood.waterfunadminservice.confirguation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.waterwood.waterfunservicecore.infrastructure.security.UserJwtAuthConverter;
import org.waterwood.waterfunservicecore.infrastructure.security.WaterJwtDecoder;

import java.util.List;

@Configuration
@EnableWebSecurity(debug = false)
public class SecurityConfig {
    private final WaterJwtDecoder jwtConverter;

    public SecurityConfig(WaterJwtDecoder jwtDecoder) {
        this.jwtConverter = jwtDecoder;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserJwtAuthConverter userJwtAuthConverter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .requireCsrfProtectionMatcher(request -> !request.getMethod().equals("GET"))
//                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
//                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                // JWT Configuration apply to interface which need to be authorized
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt
                                .decoder(jwtConverter)
                                .jwtAuthenticationConverter(userJwtAuthConverter)))
                .formLogin(form -> form.disable());
        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "http://localhost:63342"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}