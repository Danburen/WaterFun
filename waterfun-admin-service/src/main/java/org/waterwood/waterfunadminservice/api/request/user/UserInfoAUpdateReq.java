package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.common.validation.Username;
import org.waterwood.waterfunservicecore.entity.user.AccountStatus;

import java.time.Instant;

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
