package org.waterwood.waterfunadminservice.dto.request.role;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class AssignRolePermReq implements Serializable {
    @NotNull
    private List<RolePermItemDTO> permsDto;
}
