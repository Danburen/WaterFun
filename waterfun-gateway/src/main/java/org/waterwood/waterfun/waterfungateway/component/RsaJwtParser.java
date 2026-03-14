package org.waterwood.waterfun.waterfungateway.component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.waterwood.common.TokenResult;

import java.security.PublicKey;

@Component
public class RsaJwtParser {
    private final PublicKey publicKey;
    private final String jwtIssuer;

    public RsaJwtParser(
            PublicKey publicKey, @Value("${jwt.issuer:waterfun}") String jwtIssuer) {
        this.publicKey = publicKey;
        this.jwtIssuer = jwtIssuer;
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
}
