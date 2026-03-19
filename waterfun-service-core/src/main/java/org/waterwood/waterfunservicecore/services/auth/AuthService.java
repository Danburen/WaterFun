package org.waterwood.waterfunservicecore.services.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.TokenPair;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;

public interface AuthService {
    ApiResponse<LoginClientData> BuildLoginResponse(HttpServletResponse response, User user, String dfp);
    TokenPair createNewTokens(long userUid, String deviceFingerprint);
    TokenResult refreshAccessToken(String refreshToken, String dfp);
}
