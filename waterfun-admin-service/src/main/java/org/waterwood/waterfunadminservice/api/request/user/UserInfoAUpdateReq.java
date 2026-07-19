package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.waterwood.common.validation.Username;
import org.waterwood.waterfunservicecore.entity.user.AccountStatus;

@Data
public class UserInfoAUpdateReq {
    private Long uid;
    @Username
    private String username;
    private AccountStatus accountStatus;
    @Size(max = 12)
    private String nickname;
    @Size(max = 255)
    private String avatarUrl;
}
