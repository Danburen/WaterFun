package org.waterwood.waterfun.waterfungateway.component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.PublicKey;

@Component
public class RsaJwtDecoder implements ReactiveJwtDecoder {
    private final PublicKey publicKey;
    private final String jwtIssuer;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RsaJwtDecoder(
            PublicKey publicKey, @Value("${jwt.issuer:waterfun}") String jwtIssuer, ReactiveRedisTemplate<String, String> redisTemplate) {
        this.publicKey = publicKey;
        this.jwtIssuer = jwtIssuer;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Parses the JWT tokenValue and returns the claims.
     * This will validateAndRemove the tokenValue signature and expiration first.
     * @param JwToken the JWT tokenValue to parse
     * @return Claims Instance
     * @throws JwtException if the tokenValue is invalid or expired
     */
    public Claims parseToken(String JwToken) throws JwtException {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(jwtIssuer)
                .build()
                .parseSignedClaims(JwToken)
                .getPayload();
    }

    public String getIssuer() {
        return jwtIssuer;
    }

    @Override
    public Mono<Jwt> decode(String token) throws JwtException {
        return Mono.fromCallable(() -> parseToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(claims -> validateToken(claims, token));
    }

    private Jwt buildJwt(Claims claims, String token) {
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .claims(c -> c.putAll(claims))
                .claim("did", claims.get("did"))
                .issuedAt(claims.getIssuedAt().toInstant())
                .expiresAt(claims.getExpiration().toInstant())
                .build();
    }

    private Mono<Jwt> validateToken(Claims claims, String token) {
        String deviceKey = String.format("token:%s:%s",
                claims.getSubject(),
                claims.get("did"));
        return redisTemplate.opsForValue()
                .get(deviceKey)
                .filter(savedJti -> savedJti.equals(claims.getId()))
                .map(_ -> claims)
                .switchIfEmpty(Mono.error(new JwtException("Invalid token")))
                .map(savedJti -> buildJwt(claims, token));
    }
}
