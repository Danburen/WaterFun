package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;

@Data
public class ReAuthReq {
    @NotNull
    private VerifyScene scene;
}
