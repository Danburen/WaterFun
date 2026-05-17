package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.req.auth.SecurityVerifyCodeDto;

@Data
public class EmailBindActivateDto {
    @Email
    private String email;
    @NotNull
    private SecurityVerifyCodeDto verify;
}
