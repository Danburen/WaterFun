package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class RemoveSingleUserRoleReq implements Serializable {
    @NotNull
    private Integer roleId;
}

