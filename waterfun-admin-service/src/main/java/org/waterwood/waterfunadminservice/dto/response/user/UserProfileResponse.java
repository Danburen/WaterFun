package org.waterwood.waterfunadminservice.dto.response.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.Gender;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

import java.io.Serializable;

/**
 * DTO for {@link UserProfile}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse implements Serializable {
    @NotNull
    private Long id;
    @Size(max = 12)
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Gender gender;
}