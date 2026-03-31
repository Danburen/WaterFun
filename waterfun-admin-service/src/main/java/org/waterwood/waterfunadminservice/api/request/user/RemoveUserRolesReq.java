package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RemoveUserRolesReq implements Serializable {
    @NotNull
    private List<Integer> roleIds;
}

