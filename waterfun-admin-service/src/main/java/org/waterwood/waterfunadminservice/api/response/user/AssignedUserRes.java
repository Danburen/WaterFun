package org.waterwood.waterfunadminservice.api.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignedUserRes implements Serializable {
    Long userUid;
    String username;
    String nickname;
    Instant assignedAt;
    Instant expiresAt;
}
