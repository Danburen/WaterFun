package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;

@Data
public class SecuritySendCodeDto {
    @NotNull
    private VerifyChannel channel;
    @NotEmpty
    private String deviceFp;
    @NotNull
    private VerifyScene scene;
}
