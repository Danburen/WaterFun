package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.req.auth.SecurityVerifyCodeDto;

@Data
public class PhoneChangeActivateDto {
    @NotBlank
    private String phone;
    @NotNull
    private SecurityVerifyCodeDto verify;
}
