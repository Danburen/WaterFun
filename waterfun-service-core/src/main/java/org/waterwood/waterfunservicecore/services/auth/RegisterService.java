package org.waterwood.waterfunservicecore.services.auth;

import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.RegisterRequest;
import org.waterwood.waterfunservicecore.entity.user.User;

public interface RegisterService {
    @Transactional
    User register(RegisterRequest body, String smsCodeKey);

    @Transactional
    User autoRegister(String target, VerifyChannel channel, VerifyScene scene, String codeKey, String code, String deviceFp);
}
