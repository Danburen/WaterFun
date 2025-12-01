package org.waterwood.waterfunadminservice.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileDTO {
    private String nickname;
    private String avatar;
    private String email_des;
    private String phone_des;
    private String bio;
}
