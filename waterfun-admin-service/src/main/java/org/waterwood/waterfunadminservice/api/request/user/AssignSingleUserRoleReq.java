package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class AssignSingleUserRoleReq implements Serializable {
    @NotNull
    private Integer roleId;

    @Future
    private Instant expiresAt;
}

