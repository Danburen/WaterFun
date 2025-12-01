package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.waterwood.common.validation.PhoneNumber;

@Data
public class SendSmsCodeRequest {
    @PhoneNumber
    @NotBlank(message = "{validation.phone.invalid}")
    private String phoneNumber;
    private CodePurpose purpose;
}
