package org.waterwood.waterfunservicecore.services.auth;

import org.waterwood.waterfunservicecore.api.auth.LoginResult;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;

public interface LoginService {
    /**
     * Normal pwd login, must not be an administrator
     * @param body {@link PwdLoginReq}
     * @param verifyUuidKey captcha verify login key
     * @return {@link LoginResult}
     */
    LoginResult login(PwdLoginReq body, String verifyUuidKey);

    /**
     * Admin pwd login the user must have thr role of "ADMIN"
     * @param body pwd login body
     * @param verifyUuidKey verify uuid key
     * @return {@link LoginResult}
     */
    LoginResult adminLogin(PwdLoginReq body, String verifyUuidKey);

    void logout(String refreshToken, String dfp);

    /***
     * Login by code, user must not be admin
     * @param dto verify code dto
     * @param codeKey code key
     * @return  login result containing user and whether it was auto-registered
     */
    LoginResult login(VerifyCodeDto dto, String codeKey);
}
