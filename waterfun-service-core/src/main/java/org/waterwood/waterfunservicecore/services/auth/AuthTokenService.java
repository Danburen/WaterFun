package org.waterwood.waterfunservicecore.services.auth;

import io.jsonwebtoken.Claims;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;

import java.util.Set;

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

    /**
     * Generate and store refresh token
     *
     * @param userUid         the user ID
     * @param deviceId        the identify of device
     * @return Token result
     */
    TokenResult genAndCacheRefToken(long userUid, String deviceId);

    RefreshTokenPayload validateRefreshToken(long userUid, String refreshToken, String dfp);

    /**
     * Validates the access token and rejects old tokens.
     * @param claims the claims
     */
    void validateAccessTokenAndRejectOld(Claims claims);

    void removeRefreshToken(long userUid, String dfp, String refreshToken);

    void removeAccessToken(Long userUid, String deviceId);

    String buildRefCacheKey(long userUid, String deviceId, String family);

    String buildRtFamiliesCacheKey(long userUid);

    String buildAccessUserDeviceKey(long userUid, String deviceId);

    String generateFamilyId();

    Set<String> getFamilyIds(long userUid);

    /**
     * Clean all zombie refresh family
     */
    void cleanZombieRefFamily();
}
