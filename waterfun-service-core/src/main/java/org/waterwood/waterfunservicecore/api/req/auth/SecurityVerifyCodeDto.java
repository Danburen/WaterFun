package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;

@Data
public class SecurityVerifyCodeDto {
    @NotNull
    private VerifyChannel channel;
    @NotBlank
    private String code;
    @NotNull
    private VerifyScene scene;
    private String deviceFp;
}
