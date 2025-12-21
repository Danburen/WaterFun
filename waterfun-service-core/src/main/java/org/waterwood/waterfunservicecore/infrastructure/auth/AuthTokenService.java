package org.waterwood.waterfunservicecore.infrastructure.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;

/**
 * A service for managing AUTH tokens.
 */
public interface AuthTokenService {
    /**
     * Generate and store access token(jti) to cache service.
     * @param userUid the user ID
     * @param deviceId device uid
     * @return {@link TokenResult}
     */
    TokenResult generateStoreNewAndRevokeOthers(Long userUid, String deviceId);

    TokenResult generateAndStoreRefreshToken(long userUid, String deviceId, long expireInSeconds);

    TokenResult generateAndStoreRefreshToken(long userUid, String deviceId);

    TokenResult RegenerateRefreshToken(String oldRefreshToken, long userUid, String deviceId);

    RefreshTokenPayload validateRefreshToken(String refreshToken, String dfp);

    /**
     * Validates the access token and rejects old tokens.
     * @param claims the claims
     */
    void validateAccessTokenAndRejectOld(Claims claims);

    Claims parseToken(String ValidatedAccessToken) throws JwtException;

    void removeRefreshToken(String refreshToken);

    void removeAccessToken(Long userUid, String deviceId);

    Long getCurrentUserUid();
}
