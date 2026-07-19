package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.user.Gender;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicProfileResp {
    Long uid;
    private UserBrief userBrief;

    private String bio;
    private Gender gender;
    private LocalDate birthday;
    private String residence;
    private Instant createdAt;

    private Long followers;
    private Long followings;
    private Long likeCount;
    private Long postCount;
}
