package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;

/**
 * A dto to verify code segment sms and  email
 */
@Data
public class VerifyCodeDto {
    @NotNull
    private VerifyChannel channel;
    @NotBlank
    private String target;
    @NotBlank
    private String code;
    @NotNull
    private VerifyScene scene;
    private String deviceFp;
}
