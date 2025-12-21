package org.waterwood.waterfunservicecore.services.auth;

import org.waterwood.api.TokenPair;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

public interface AuthService {
    TokenPair createNewTokens(long userUid, String deviceFingerprint);
    TokenResult refreshAccessToken(String refreshToken, String dfp);
}
