package org.waterwood.waterfunservicecore.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.AccountStatus;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link User}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse implements Serializable {
    private String uid;
    private String username;
    private String nickname;
    private AccountStatus accountStatus;
    private Instant createdAt;
    private Boolean passwordHash;
}