package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class EmailLoginRequestBody {
    @Email(message = "{verification.email_address.invalid}")
    private String email;
    private String emailCode;
    private String deviceFp;
}
