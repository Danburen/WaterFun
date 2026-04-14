package org.waterwood.waterfunadminservice.api.request.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteRolesRequest {
    private List<Integer> roleIds;
}
