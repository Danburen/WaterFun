package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;

@Data
@NoArgsConstructor
public class SendCodeDto {
    @NotBlank(message = "{valid.need_target}")
    private String target;
    @NotNull
    private VerifyChannel channel;
    private String deviceFp;
    @NotNull
    private VerifyScene scene;
}
