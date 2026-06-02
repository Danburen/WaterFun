package org.waterwood.waterfunservicecore.services.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.waterwood.api.TokenPair;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;

public interface AuthCoreService {
    LoginClientData BuildLoginResponse(HttpServletResponse response, User user, String dfp);
    TokenPair createNewTokens(long userUid, String deviceFingerprint);
    TokenPair refreshAccessToken(String refreshToken, String dfp);
}
