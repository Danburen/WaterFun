package org.waterwood.waterfunadminservice.api.request.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteRolePermsReq {
    private List<Integer> ids;
}
