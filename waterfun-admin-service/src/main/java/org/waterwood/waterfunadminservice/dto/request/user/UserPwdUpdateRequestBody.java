package org.waterwood.waterfunadminservice.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.waterwood.common.validation.StrongPassword;

@Data
public class UserPwdUpdateRequestBody {
    @NotBlank
    private String oldPwd;
    @NotBlank
    @StrongPassword
    private String newPwd;
    @NotBlank
    private String confirmPwd;
}
