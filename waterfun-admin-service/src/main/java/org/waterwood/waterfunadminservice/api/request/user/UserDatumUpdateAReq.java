package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.waterwood.common.validation.PhoneNumber;

@Data
public class UserDatumUpdateAReq {
    @Email
    private String email;
    @PhoneNumber
    private String phone;
}
