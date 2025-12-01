package org.waterwood.waterfunadminservice.dto.request.role;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class UpdateRolePermReq implements Serializable {
    private List<RolePermItemDTO> permsDto;
}
