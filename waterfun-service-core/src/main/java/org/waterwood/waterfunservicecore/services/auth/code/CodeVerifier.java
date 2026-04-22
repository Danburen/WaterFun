package org.waterwood.waterfunservicecore.services.auth.code;

import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;

public interface CodeVerifier {
    Object generateVerifyCode();
    boolean verifyCode(String target,VerifyScene scene, String key, String code);
    VerifyChannel  channel();
}
