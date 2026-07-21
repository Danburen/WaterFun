package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.TokenInvalidOrExpireException;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.services.auth.SingleUseTokenService;

import java.time.Duration;
import java.util.UUID;

/**
 * One-time re-authentication token service.
 * <p>Used for sensitive operations (change phone/email, reset password, etc.)
 * to prove the user has completed a fresh SMS/email verification.</p>
 *
 * Redis key: {@code op:re-auth:{scene}:{uuid}}
 * Value: userUid (String)
 * TTL: 5 minutes, consumed on first use ({@link SingleUseTokenService#consumeVerifyToken}).
 */
@Service
@RequiredArgsConstructor
public class SingleUseTokenServiceImpl implements SingleUseTokenService {

    private static final String KEY_PREFIX = "op:re-auth:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final RedisHelperHolder redisHelper;

    @Override
    public TokenResult generateVerifyToken(String scene, Long userUid) {
        String token = KEY_PREFIX + scene + ":" + UUID.randomUUID();
        redisHelper.set(token, String.valueOf(userUid), TOKEN_TTL);
        return new TokenResult(token, TOKEN_TTL.toSeconds());
    }

    @Override
    public Long consumeVerifyToken(String token, String scene) {
        if (token == null || token.isBlank()) {
            throw new TokenInvalidOrExpireException();
        }
        // Validate the key prefix contains the expected scene
        String expectedPrefix = KEY_PREFIX + scene + ":";
        if (!token.startsWith(expectedPrefix)) {
            throw new TokenInvalidOrExpireException();
        }
        String val = redisHelper.getAndDel(token);
        if (val == null) {
            throw new TokenInvalidOrExpireException();
        }
        return Long.parseLong(val);
    }
}
