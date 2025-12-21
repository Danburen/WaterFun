package org.waterwood.waterfunservicecore.services.auth.code;

import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SecuritySendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.SecurityVerifyCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.common.exceptions.BusinessException;

/**
 * A service for sending verification codes
 * usually use in Strong Authentication
 * .e.g SMS, Email
 */
public interface VerificationService {

    /**
     * Send verification code to authenticate user.
     * will check the db and find channel sending target linked to target
     * @param userUid user id
     * @param channel channel
     * @param scene scene
     * @return code result
     */
    CodeResult sendAutoTargetAuthenticationCode(long userUid, VerifyChannel channel, VerifyScene scene);

    /**
     * Send verification code to authenticate user with given target
     * @param channel channel
     * @param scene  scene
     * @param target target
     * @return code result
     */
    CodeResult sendAuthenticationCode(String target, VerifyChannel channel, VerifyScene scene);

    CodeResult sendCode(SendCodeDto dto);

    /**
     * Verify code
     * @param target target
     * @param scene  scene
     * @param channel channel
     * @param key key of the code
     * @param code  code
     * @throws BusinessException if code is invalid
     */
    void verifyCode(String target, VerifyScene scene, VerifyChannel channel, String key, String code);

    void verifyCode(String verifyCodeKey, VerifyCodeDto verifyBody);

    /**
     * Verify code with point target, will check the scene whether is the same as the scene
     * <b>Usually used for checking the user whether is the account owner</b> with <b>whatever</b> channel.
     * @param verifyCodeKey key of the code
     * @param verifyBody verify body
     * @param scene target scene
     */
    void verifyAuthorizedCode(String verifyCodeKey, SecurityVerifyCodeDto verifyBody, String target, VerifyScene scene);

    /**
     * Verify code with point target, will check the scene whether is the same as the scene
     * <b>Usually used for verify the new binding target</b> with <b>target</b>channel.
     * @param verifyCodeKey key of the code
     * @param verifyBody verify body
     * @param scene target scene
     * @param allowChannels target channel
     */
    void verifyAuthorizedCodeWithChannel(String verifyCodeKey, SecurityVerifyCodeDto verifyBody, String target, VerifyScene scene, VerifyChannel... allowChannels);
}
