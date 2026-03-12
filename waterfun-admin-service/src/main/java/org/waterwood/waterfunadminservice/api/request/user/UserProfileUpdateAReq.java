package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.Gender;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Update DTO for {@link UserProfile}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileUpdateAReq implements Serializable {
    @Size(max = 500)
    private String bio;
    private Gender gender;
    private LocalDate birthDate;
    @Size(max = 50)
    private String residence;
}