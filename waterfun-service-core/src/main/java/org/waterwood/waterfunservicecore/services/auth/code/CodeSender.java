package org.waterwood.waterfunservicecore.services.auth.code;

import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

/**
 * A class to manager verification code sending
 */
public interface CodeSender {
    /**
     * Direct Send code to target and save to cache with key.
     * @param target  target usually is phone or email
     * @param scene verify scene
     * @return  result
     */
    CodeResult sendCode(String target, VerifyScene scene);

    /**
     * The channel of the sender
     * @return channel
     */
    VerifyChannel channel();
}
