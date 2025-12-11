package org.waterwood.waterfunservicecore.api.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;

@Data
public class EmailChangeDto {
    @Email
    private final String email;
    @NotNull
    private final VerifyCodeDto verify;
}
