package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.Instant;

@Data
@EqualsAndHashCode(of = "permissionId")
public class UserPermItemDto implements Serializable {
    @NotNull
    private Integer permissionId;

    @Future
    private Instant expiresAt;
}

