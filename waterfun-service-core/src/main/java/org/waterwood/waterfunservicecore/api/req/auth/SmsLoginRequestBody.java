package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.waterwood.common.validation.PhoneNumber;

@Data
public class SmsLoginRequestBody {
    @NotBlank(message = "{verification.phone.invalid}")
    @PhoneNumber
    private String phoneNumber;
    @NotBlank(message = "{validation.not_empty}")
    private String smsCode;
    @NotBlank(message = "{auth.device_fingerprint.required}")
    private String deviceFp;
}
