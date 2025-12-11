package org.waterwood.waterfunservicecore.services.auth.code;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.services.sms.SmsCodeService;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    private final CodeSenderFactory codeSenderFactory;
    private final SmsCodeService smsCodeService;
    private final CodeVerifierFactory codeVerifierFactory;

    @Override
    public CodeResult sendAuthorizedCode(SendCodeDto dto) {
        if(! VerifyScene.isPublicScene(dto.getScene())){
            throw new BusinessException(BaseResponseCode.INVALID_VERIFY_SCENE);
        }
        return sendCode(dto);
    }

    @Override
    public CodeResult sendCode(SendCodeDto dto) {
        CodeSender sender = codeSenderFactory.of(dto.getChannel());
        return sender.sendCode(dto.getTarget(), dto.getScene());
    }

    @Override
    public void verifyCode(String target, VerifyScene scene, VerifyChannel channel, String key, String code) {
        CodeVerifier verifier = codeVerifierFactory.of(channel);
        if(! verifier.verifyCode(target, scene, key, code)){
            throw new BusinessException(BaseResponseCode.VERIFY_CODE_INVALID);
        }
    }

    @Override
    public void verifyCode(String verifyCodeKey, VerifyCodeDto verifyBody){
        this.verifyCode(
                verifyBody.getTarget(),
                verifyBody.getScene(),
                verifyBody.getChannel(),
                verifyCodeKey,
                verifyBody.getCode());
    }
}
