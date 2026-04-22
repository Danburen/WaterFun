package org.waterwood.waterfunservicecore.services.auth.code;

import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SecurityVerifyCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.common.exceptions.BizException;

/**
 * A service for sending verification codes
 * usually use in Strong Authentication
 * .e.g SMS, Email
 */
public interface VerificationService {

    /**
     * Send verification code to authenticate user.
     * will check the db and find channel sending target linked to target
     *
     * @param channel channel
     * @param scene   scene
     * @return code result
     */
    CodeResult sendAutoTargetAuthenticationCode(VerifyChannel channel, VerifyScene scene);

    /**
     * Send verification code to authenticate user segment given target
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
     * @param key key ofPending the code
     * @param code  code
     * @throws BizException if code is invalid
     */
    void verifyCode(String target, VerifyScene scene, VerifyChannel channel, String key, String code);

    void verifyCode(String verifyCodeKey, VerifyCodeDto verifyBody);

    /**
     * Verify code segment point target, will check the scene whether is the same as the scene
     * <b>Usually used for checking the user whether is the account owner</b> segment <b>whatever</b> channel.
     * @param verifyCodeKey key ofPending the code
     * @param verifyBody verify body
     * @param scene target scene
     */
    void verifyAuthorizedCode(String verifyCodeKey, SecurityVerifyCodeDto verifyBody, String target, VerifyScene scene);

    /**
     * Verify code segment point target, will check the scene whether is the same as the scene
     * <b>Usually used for verify the new binding target</b> segment <b>target</b>channel.
     * @param verifyCodeKey key ofPending the code
     * @param verifyBody verify body
     * @param scene target scene
     * @param allowChannels target channel
     */
    void verifyAuthorizedCodeWithChannel(String verifyCodeKey, SecurityVerifyCodeDto verifyBody, String target, VerifyScene scene, VerifyChannel... allowChannels);
}
