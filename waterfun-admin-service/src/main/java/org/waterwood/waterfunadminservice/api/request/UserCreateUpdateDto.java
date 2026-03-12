package org.waterwood.waterfunadminservice.api.request;

import lombok.Data;
import org.waterwood.common.validation.Username;

@Data
public class UserCreateUpdateDto {
    @Username
    private String username;
    private String nickname;
}
