package org.waterwood.waterfunadminservice.api.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.UserCounter;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link UserCounter}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCounterARes implements Serializable {
    private Byte level;
    private Integer exp;
    private Integer followerCnt;
    private Integer followingCnt;
    private Integer likeCnt;
    private Integer postCnt;
    private Instant updatedAt;
    private Short visible;
}