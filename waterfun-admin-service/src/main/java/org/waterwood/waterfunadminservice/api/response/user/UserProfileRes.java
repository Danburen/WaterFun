package org.waterwood.waterfunadminservice.api.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.Gender;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO for {@link UserProfile}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileRes implements Serializable {
    private String bio;
    private Gender gender;
    private LocalDate birthDate;
    private String residence;
    private Instant updateAt;
}