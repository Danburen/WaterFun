package org.waterwood.waterfunservicecore.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.services.auth.impl.TokenService;

@Component
@Deprecated
public class WaterJwtDecoder implements Converter<String, Jwt> , JwtDecoder {
    private final RsaJwtUtil rsaJwtUtil;
    private final TokenService tokenService;

    public WaterJwtDecoder(RsaJwtUtil rsaJwtUtil, TokenService tokenService) {
        this.rsaJwtUtil = rsaJwtUtil;
        this.tokenService = tokenService;
    }

    @Override
    public Jwt convert(@NotNull String token) {
            Claims claims = rsaJwtUtil.parseToken(token);
            tokenService.validateAccessTokenAndRejectOld(claims);

            return Jwt.withTokenValue(token)
                    .header("alg","RS256")
                    .header("typ","JWT")
                    .claims(c -> c.putAll(claims))
                    .issuedAt(claims.getIssuedAt().toInstant())
                    .expiresAt(claims.getExpiration().toInstant())
                    .build();
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        return this.convert(token);
    }
}
