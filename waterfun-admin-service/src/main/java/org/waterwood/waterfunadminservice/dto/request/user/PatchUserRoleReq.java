package org.waterwood.waterfunadminservice.dto.request.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class PatchUserRoleReq implements Serializable {
    private List<UserRoleItemDto> adds;
    private List<Integer> deletePermIds;
}
