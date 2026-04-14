package org.waterwood.waterfunadminservice.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;

/**
 * A interface for admin side auth
 */
public interface AuthService {
    /**
     * Login by password
     * @param body the request body
     * @param request servlet request which contains cookies
     * @param response servlet response which need to set up cookies
     * @return Login client data which contains user info and tokens
     */
    LoginClientData loginByPwd(PwdLoginReq body, HttpServletRequest request, HttpServletResponse response);
}
