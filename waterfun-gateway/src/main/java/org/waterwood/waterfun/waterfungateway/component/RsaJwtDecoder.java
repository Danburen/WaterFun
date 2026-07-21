package org.waterwood.waterfun.waterfungateway.component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.waterwood.api.AuthCode;
import org.waterwood.common.constratin.UserKeyBuilder;
import org.waterwood.common.exceptions.AuthException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.PublicKey;

@Slf4j
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
     * Parses the JWT value and returns the claims.
     * This will validateAndRemove the value signature and expiration first.
     * @param JwToken the JWT value to parse
     * @return Claims Instance
     * @throws JwtException if the value is invalid or expired
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
    public Mono<Jwt> decode(String token) {
        if (token == null || token.isEmpty())
            return Mono.error(new AuthException(AuthCode.TOKEN_MISSING));
        return Mono.fromCallable(() -> parseToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(claims -> validateToken(claims, token))
                .onErrorResume(e ->
                        e instanceof AuthException
                                ? Mono.error(e)
                                : Mono.error(new AuthException(AuthCode.TOKEN_INVALID))
                );
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
        Object didObj = claims.get("did");
        if (didObj == null) {
            return Mono.error(new AuthException(AuthCode.TOKEN_INVALID));
        }
        String deviceKey = UserKeyBuilder.userAccessDevice(
                Long.parseLong(claims.getSubject()),
                didObj.toString()
        );
//        log.info("Validating token for user: {}, device: {}, jti: {}",
//                claims.getSubject(), claims.get("did"), claims.getId());
        return redisTemplate.opsForValue()
                .get(deviceKey)
                .filter(savedJti -> savedJti.equals(claims.getId()))
                .map(_ -> claims)
                .switchIfEmpty( // Token is invalid if jti does not match or no jti found
                        Mono.error(new AuthException(AuthCode.TOKEN_INVALID))
                )
                .map(savedJti -> buildJwt(claims, token));
    }
}
