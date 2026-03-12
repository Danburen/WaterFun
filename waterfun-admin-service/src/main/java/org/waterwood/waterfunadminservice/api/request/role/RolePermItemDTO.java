package org.waterwood.waterfunadminservice.api.request.role;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class RolePermItemDTO implements Serializable {
    @NotNull
    private Integer permissionId;
    @Future
    private LocalDate expiresAt;
}
