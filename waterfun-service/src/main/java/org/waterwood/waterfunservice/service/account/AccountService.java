package org.waterwood.waterfunservice.service.account;


import org.waterwood.waterfunservice.api.request.*;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

public interface AccountService {
    /**
     * Update password after authenticated
     *
     * @param dto password update dto
     */
    void changePwd(String verifyCodeKey, ResetPasswordDto dto);

    /**
     * Set  password
     *
     * @param verifyCodeKey cached verify code key
     * @param dto           password update dto
     */
    void setPassword(String verifyCodeKey, SetPasswordDto dto);
    /**
     * Bind or activate email
     *
     * @param verifyCodeKey cached verify code key
     * @param dto           verify email dto
     */
    void activateEmail(String verifyCodeKey, EmailBindActivateDto dto);

    /**
     * Change email
     *
     * @param verifyCodeKey cached verify code key
     * @param dto           change email dto
     */
    CodeResult changeEmail(String verifyCodeKey, EmailChangeDto dto);

    /**
     * Bind a email for  user
     *
     * @param verifyCodeKey cached verify code key
     * @param dto           bind email dto
     */
    CodeResult bindEmail(String verifyCodeKey, EmailBindActivateDto dto);

    /**
     * Clean up unverified email
     */
    void cleanUnverifiedEmail();

    /**
     * Change phone number, only verify and check old and send activate code to new phone.
     * won't change phone number to db.
     *
     * @param channelVerifyCodeKey cached verify code key
     * @param dto                  change phone number dto
     */
    CodeResult changePhone(String channelVerifyCodeKey, PhoneChangeActivateDto dto);

    /**
     * Activate phone number
     *
     * @param verifyCodeKey cached verify code key
     * @param dto           change phone number dto
     */
    void activatePhone(String verifyCodeKey, PhoneChangeActivateDto dto);

    /**
     * Verify user's account for Unbinding email
     *
     * @param channelVerifyCodeKey verify body
     * @param dto                  unbind email dto
     */
    void unbindEmail(String channelVerifyCodeKey, EmailBindActivateDto dto);


}
