package org.waterwood.waterfunservicecore.services.auth;

import org.waterwood.api.TokenPair;
import org.waterwood.common.TokenResult;

public interface AuthService {
    TokenPair createNewTokens(long userId, String deviceFingerprint);
    TokenResult refreshAccessToken(String refreshToken, String dfp);
}
