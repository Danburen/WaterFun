package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneChangeVerifyReq {
    @NotBlank
    private String verifyKey;
    @NotBlank
    private String code;
}
