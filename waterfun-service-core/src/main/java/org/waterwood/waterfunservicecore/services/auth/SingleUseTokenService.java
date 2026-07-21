package org.waterwood.waterfunservicecore.services.auth;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.exception.BizException;

public interface SingleUseTokenService {
    /**
     * Generate a one-time token for the given scene and user.
     *
     * @param scene   operation scene (e.g. "FORGOT_PASSWORD", "CHANGE_PHONE")
     * @param userUid authenticated user's uid
     * @return {@link TokenResult}
     */
    TokenResult generateVerifyToken(String scene, Long userUid);

    /**
     * Consume a one-time token for the given scene.
     * <p>The token is deleted atomically after read (one-time use).</p>
     *
     * @param token the token string to consume
     * @param scene expected scene (e.g. "FORGOT_PASSWORD")
     * @return the userUid associated with the token
     * @throws BizException with {@link BaseResponseCode#INVALID_TOKEN_OR_EXPIRED} if invalid/expired
     */
    Long consumeVerifyToken(String token, String scene);
}
