package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.common.validation.StrongPassword;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;

@Data
public class ForgetPasswordDto {
    @NotNull
    private VerifyChannel channel;
    @NotBlank
    private String target;
    @NotBlank
    private String code;
    @NotBlank
    @StrongPassword
    private String newPwd;
    @NotBlank
    private String confirmPwd;
    private DeviceInfoReq deviceInfo;
}
