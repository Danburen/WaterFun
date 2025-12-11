package org.waterwood.waterfunservicecore.services.auth.code;

import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
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
    CodeResult sendAuthorizedCode(SendCodeDto dto);
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
}
