package org.waterwood.waterfunservicecore.services.auth.code;

import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;

public interface CodeVerifier {
    Object generateVerifyCode();
    boolean verifyCode(String target,VerifyScene scene, String key, String code);
    VerifyChannel  channel();
}
