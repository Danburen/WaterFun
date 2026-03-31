package org.waterwood.waterfunadminservice.api.request.role;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.Instant;

@Data
@EqualsAndHashCode(of = "permissionId")
public class RolePermItemDTO implements Serializable {
    @NotNull
    private Integer permissionId;
    @Future
    private Instant expiresAt;
}
