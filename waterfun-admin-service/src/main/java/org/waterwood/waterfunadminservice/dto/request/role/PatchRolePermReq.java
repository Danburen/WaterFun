package org.waterwood.waterfunadminservice.dto.request.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.waterwood.waterfunadminservice.dto.request.role.RolePermItemDTO;
import org.waterwood.waterfunadminservice.infrastructure.validation.AtLeastOneNotNull;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@AtLeastOneNotNull(fields = {"updates", "deletePermIds"})
public class PatchRolePermReq implements Serializable {
    private List<RolePermItemDTO> updates;
    private List<Integer> deletePermIds;
}
