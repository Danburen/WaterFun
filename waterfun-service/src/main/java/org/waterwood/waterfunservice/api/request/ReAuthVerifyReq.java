package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;

@Data
public class ReAuthVerifyReq {
    @NotNull
    private VerifyScene scene;

    @NotBlank
    private String code;
}
