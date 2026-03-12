package org.waterwood.waterfunadminservice.api.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class SmsCodeResult {
    private final boolean sendSuccess;
    private final String phoneNumber;
    private final @Nullable String message;
    private final @Nullable String responseRaw;

    protected String key;

    public static SmsCodeResult success() {
        return new SmsCodeResult(true,null,null,null,null);
    }
}
