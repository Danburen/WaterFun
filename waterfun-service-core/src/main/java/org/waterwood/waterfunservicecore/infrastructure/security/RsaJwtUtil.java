package org.waterwood.waterfunservicecore.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.waterwood.common.TokenResult;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Component
public class RsaJwtUtil {
    private static final String JWT_ISSUER = "waterfun";

    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public RsaJwtUtil(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public TokenResult generateToken(Map<String,Object> claims, Duration dur){
        Date expDate = new Date(System.currentTimeMillis() + dur.toMillis());

        return new TokenResult(Jwts.builder()
                .claims(claims) //sub
                .issuer(JWT_ISSUER) //iss
                .issuedAt(new Date()) //iat
                .expiration(expDate)  //exp
                //.id() //itj
                .signWith(privateKey,Jwts.SIG.RS256)
                .compact(), dur.toSeconds());
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
                .build()
                .parseSignedClaims(JwToken)
                .getPayload();
    }

    public boolean validateToken(String compactJws){
        try {
            Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(compactJws);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String getIssuer() {
        return JWT_ISSUER;
    }
}
