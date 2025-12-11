package org.waterwood.waterfunservicecore.api.resp.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.api.VerifyChannel;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class CodeResult {
    private final boolean sendSuccess;
    private final String target;
    private final @Nullable String message;
    private final @Nullable String responseRaw;
    private final VerifyChannel channel;

    protected String key;

    public static CodeResult success(String target, VerifyChannel channel) {
        return CodeResult.builder().sendSuccess(true).target(target).channel(channel).build();
    }

    public static CodeResult fail(String target, VerifyChannel channel, String message) {
        return CodeResult.builder().sendSuccess(false).target(target).channel(channel).message(message).build();
    }
}
