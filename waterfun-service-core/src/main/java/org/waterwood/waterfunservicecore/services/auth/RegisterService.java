package org.waterwood.waterfunservicecore.services.auth;

import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.api.req.auth.RegisterRequest;
import org.waterwood.waterfunservicecore.entity.user.User;

public interface RegisterService {
    @Transactional
    User register(RegisterRequest body, String smsCodeKey);
}
