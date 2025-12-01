package org.waterwood.waterfunadminservice.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.Gender;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

import java.io.Serializable;

/**
 * Update DTO for {@link UserProfile}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserProfileRequest implements Serializable {
    @Size(max = 12)
    private String nickname;
    private String avatarUrl;
    @Size(max = 500)
    private String bio;
    private Gender gender;
}