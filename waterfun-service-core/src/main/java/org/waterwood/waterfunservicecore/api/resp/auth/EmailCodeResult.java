package org.waterwood.waterfunservicecore.api.resp.auth;

import lombok.*;
import org.jetbrains.annotations.Nullable;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class EmailCodeResult {
    private boolean sendSuccess;
    private @Nullable final String email;
    private @Nullable String message;
    private @Nullable String responseRaw;

    private String key;
    public static EmailCodeResult success() {
        return new EmailCodeResult(true,null,null,null,null);
    }

    public static EmailCodeResult success(String target) {
        return new EmailCodeResult(true,target,null,null,null);
    }

    public static EmailCodeResult fail(){
        return new EmailCodeResult(false,null,null,null,null);
    }
}
