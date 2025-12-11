package org.waterwood.waterfunservicecore.services.account;


import org.waterwood.waterfunservicecore.api.req.EmailChangeDto;
import org.waterwood.waterfunservicecore.api.req.ResetPasswordDto;
import org.waterwood.waterfunservicecore.api.req.SetPasswordDto;
import org.waterwood.waterfunservicecore.api.req.EmailBindActivateDto;

public interface AccountService {
    /**
     * Update password after authenticated
     * @param userId user id
     * @param dto password update dto
     */
    void changePwd(Long userId, String verifyCodeKey,ResetPasswordDto dto);

    /**
     * Set  password
     * @param userId user id
     * @param verifyCodeKey cached verify code key
     * @param dto password update dto
     */
    void setPassword(Long userId, String verifyCodeKey,SetPasswordDto dto);
    /**
     * Bind or activate email
     * @param userId user id
     * @param verifyCodeKey cached verify code key
     * @param dto verify email dto
     */
    void bindOrActivateEmail(Long userId, String verifyCodeKey, EmailBindActivateDto dto);

    /**
     * Change email
     * @param userId user id
     * @param verifyCodeKey cached verify code key
     * @param dto change email dto
     */
    void changeEmail(long userId, String verifyCodeKey, EmailChangeDto dto);
}
