package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordChangeReq {
    @NotBlank
    private String reAuthToken;
    @NotBlank
    private String newPwd;
    @NotBlank
    private String confirmPwd;
}
