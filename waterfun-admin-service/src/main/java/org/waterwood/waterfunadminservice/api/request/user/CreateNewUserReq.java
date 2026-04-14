package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.common.validation.PhoneNumber;
import org.waterwood.common.validation.StrongPassword;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewUserReq {
    @PhoneNumber
    private String phone;
    @NotBlank(message = "{user.valid.username_invalid}")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "{user.valid.username_invalid}")
    private String username;
    @StrongPassword
    private String password;
    @NotNull
    private Short userType;
}
