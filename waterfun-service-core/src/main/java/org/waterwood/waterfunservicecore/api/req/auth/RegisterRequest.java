package org.waterwood.waterfunservicecore.api.req.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.waterwood.common.validation.StrongPassword;

@Data
public class RegisterRequest {
    @JsonProperty("phone")
    private String phoneNumber;
    @NotBlank(message = "{user.validation.username_invalid}")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "{user.validation.username_invalid}")
    private String username;
    private String smsCode;
    @NotBlank(message = "{auth.device_fingerprint.required}")
    private String deviceFp;
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "{user.password.pattern}"
    )
    @StrongPassword
    private String password;
    @Email(message = "{verification.email_address.invalid")
    private String email;
}
