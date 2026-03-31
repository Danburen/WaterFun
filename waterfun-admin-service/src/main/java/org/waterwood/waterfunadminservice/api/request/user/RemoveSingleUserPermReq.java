package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class RemoveSingleUserPermReq implements Serializable {
    @NotNull
    private Integer permissionId;
}

