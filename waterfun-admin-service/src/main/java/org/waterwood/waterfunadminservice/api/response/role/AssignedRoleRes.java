package org.waterwood.waterfunadminservice.api.response.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignedRoleRes {
    private Integer id;
    private String code;
    private String name;
    private Instant assignedAt;
    private Instant expiresAt;
}
