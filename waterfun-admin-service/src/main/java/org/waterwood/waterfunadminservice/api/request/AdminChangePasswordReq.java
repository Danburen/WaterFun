package org.waterwood.waterfunadminservice.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.waterwood.common.validation.StrongPassword;

@Data
public class AdminChangePasswordReq {
    @NotBlank
    private String oldPwd;
    @NotBlank
    @StrongPassword
    private String newPwd;
    @NotBlank
    private String confirmPwd;
}
