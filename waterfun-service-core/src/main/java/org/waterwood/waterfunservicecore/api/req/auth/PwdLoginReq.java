package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.waterwood.common.validation.StrongPassword;

@Data
public class PwdLoginReq {
    @NotEmpty(message = "{validation.required}")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "{user.username.pattern}")
    private String username;
    @NotBlank(message = "{validation.required}")
    @Size(min = 8, max = 20, message = "{validation.size}")
    @StrongPassword
    private String password;
    @NotEmpty(message = "{validation.required}")
    private String captcha;
    @NotEmpty(message = "{auth.device_fingerprint.required}")
    private String deviceFp;
}

