package org.waterwood.waterfunadminservice.api.response.user;

import jakarta.validation.constraints.Size;
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
public class UserInfoARes implements Serializable {
    private Long uid;
    private String username;
    private AccountStatus accountStatus;
    private Instant statusChangedAt;
    private Instant updatedAt;
    private Instant createdAt;
    private String nickname;
    private String avatarUrl;
    private Instant lastActiveAt;
}