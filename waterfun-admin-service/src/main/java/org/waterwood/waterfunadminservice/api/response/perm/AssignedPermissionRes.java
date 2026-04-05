package org.waterwood.waterfunadminservice.api.response.perm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignedPermissionRes implements Serializable {
    private Integer id;
    private String code;
    private String name;
    private Instant assignedAt;
    private Instant expiresAt;
}
