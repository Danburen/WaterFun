package org.waterwood.waterfunservicecore.services.auth;

import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.entity.user.User;

public interface LoginService {
    User login(PwdLoginReq body, String verifyUUIDKey);

    boolean logout(long userUid ,String refreshToken, String dfp);

    /***
     * Login by code
     * @param dto verify code dto
     * @param codeKey code key
     * @return  user
     */
    User login(VerifyCodeDto dto, String codeKey);
}
