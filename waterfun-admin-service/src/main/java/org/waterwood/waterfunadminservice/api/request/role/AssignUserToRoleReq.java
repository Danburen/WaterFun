package org.waterwood.waterfunadminservice.api.request.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserToRoleReq {
    private List<Long> userUids;
    private Instant expiresAt;
}
