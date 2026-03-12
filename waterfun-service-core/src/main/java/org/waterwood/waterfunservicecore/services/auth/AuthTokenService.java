package org.waterwood.waterfunservicecore.services.auth;

import io.jsonwebtoken.Claims;
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
    TokenResult genCacheNewAccTokenRevokeOlds(Long userUid, String deviceId);

    TokenResult genAndCacheRefToken(long userUid, String deviceId, long expireInSeconds);

    TokenResult genAndCacheRefToken(long userUid, String deviceId);

    TokenResult ReGenRefreshToken(String oldRefreshToken, long userUid, String deviceId);

    RefreshTokenPayload validateRefreshToken(long userUid, String refreshToken, String dfp);

    /**
     * Validates the access token and rejects old tokens.
     * @param claims the claims
     */
    void validateAccessTokenAndRejectOld(Claims claims);

    void removeRefreshToken(long userUid, String refreshToken);

    void removeAccessToken(Long userUid, String deviceId);

    String buildRefCacheKey(long userUid, String refToken);

    String buildAccCacheKey(long userUid, String deviceId);
}
