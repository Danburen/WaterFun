package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.waterwood.common.validation.PhoneNumber;
import org.waterwood.common.validation.StrongPassword;

@Data
public class RegisterRequest {
    @PhoneNumber
    @NotBlank(message = "{valid.phone.invalid}")
    private String phone;
    @NotBlank(message = "{user.valid.username_invalid}")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "{user.valid.username_invalid}")
    private String username;
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "{user.password.pattern}"
    )
    @StrongPassword
    private String password;
    @Email(message = "{verification.email_address.invalid")
    private String email;
    @NotNull
    private VerifyCodeDto verify;
}
