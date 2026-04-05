package org.waterwood.waterfunadminservice.api.request.perm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class assignPermToUsersReq implements Serializable {
    private List<Long> ids;
    private Instant expiresAt;
}
