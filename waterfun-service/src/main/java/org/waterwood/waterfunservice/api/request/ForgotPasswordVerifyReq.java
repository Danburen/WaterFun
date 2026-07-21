package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordVerifyReq {
    @NotBlank
    private String reAuthKey;
    @NotBlank
    private String code;
}
