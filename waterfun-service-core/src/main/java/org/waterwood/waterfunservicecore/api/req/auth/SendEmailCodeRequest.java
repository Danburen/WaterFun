package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendEmailCodeRequest {
    @Email(message = "{validation.email_address.invalid}")
    @NotBlank(message = "{validation.email_address.invalid}")
    private String email;
    private CodePurpose purpose;
}
