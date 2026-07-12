package org.waterwood.waterfunservicecore.services.auth;

import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.entity.user.User;

public interface LoginService {
    User login(PwdLoginReq body, String verifyUUIDKey);

    void logout(String refreshToken, String dfp);

    /***
     * Login by code
     * @param dto verify code dto
     * @param codeKey code key
     * @return  login result containing user and whether it was auto-registered
     */
    LoginResult login(VerifyCodeDto dto, String codeKey);

    record LoginResult(User user, boolean isNewUser) {}
}
