package org.waterwood.waterfunservicecore.services.auth.code;

import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

/**
 * A class to manager verification code sending
 */
public interface CodeSender {
    CodeResult sendCode(String target, VerifyScene scene);
    VerifyChannel channel();
}
