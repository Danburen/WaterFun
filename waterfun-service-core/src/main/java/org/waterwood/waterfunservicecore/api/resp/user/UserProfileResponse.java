package org.waterwood.waterfunservicecore.api.resp.user;

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
    private String bio;
    private Gender gender;
    private String birthday;
    private String residence;
}