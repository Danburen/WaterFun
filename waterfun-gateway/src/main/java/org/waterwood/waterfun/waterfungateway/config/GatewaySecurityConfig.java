package org.waterwood.waterfun.waterfungateway.config;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.waterwood.waterfun.waterfungateway.component.RsaJwtDecoder;
import org.waterwood.waterfun.waterfungateway.exception.JsonAccessDeniedHandler;
import org.waterwood.waterfun.waterfungateway.exception.JsonAuthenticationEntryPoint;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    private PublicKey publicKey;
    private final RsaJwtDecoder jwtParser;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    @Value("${waterfun.cors.allowed-origin-patterns:https://waterfun.top,http://localhost:*,http://127.0.0.1:*}")
    private String allowedOrigins;

    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtParser)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/api/admin/auth/**").permitAll()
                        // Private GET (must precede public wildcards)
                        .pathMatchers(HttpMethod.GET, "/api/posts/me/**").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/post/tags/me").authenticated()
                        // Public GET read-only
                        .pathMatchers(HttpMethod.GET, "/api/posts/*", "/api/posts/hot", "/api/posts/*/liked-users").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/banners", "/api/banners/by-position").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/announcements").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/post/category/options").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/post/tags/hot", "/api/post/tags/search/**", "/api/post/tags/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/user/*/profile", "/api/user/*/card", "/api/user/*/avatar").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/user/*/followers", "/api/user/*/followings").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/resource/legal/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(parseCsv(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter(){
//        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
//        authoritiesConverter.setAuthorityPrefix("ROLE_");
//        authoritiesConverter.setAuthoritiesClaimName("roles");

//        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
//        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
//        converter.setPrincipalClaimName("sub");
//        return new ReactiveJwtAuthenticationConverterAdapter(converter);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
