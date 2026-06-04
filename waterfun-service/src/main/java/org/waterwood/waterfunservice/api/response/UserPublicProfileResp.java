package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.Gender;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicProfileResp implements Serializable {
    private Gender gender;
    private Short level;
    private Long exp;
    private Integer followerCount;
    private Integer followingCount;
    private Integer postCount;
    private Integer likeCount;
    private Instant createdAt;
}
