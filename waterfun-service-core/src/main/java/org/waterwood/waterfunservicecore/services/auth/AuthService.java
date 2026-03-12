package org.waterwood.waterfunservicecore.services.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.TokenPair;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;

public interface AuthService {
    ApiResponse<LoginClientData> BuildLoginResponse(HttpServletResponse response, User user, String dfp);
    TokenPair createNewTokens(long userUid, String deviceFingerprint);
    TokenResult refreshAccessToken(long userUid,String refreshToken, String dfp);
}
