package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AssignUserRoleReq {
    @NotNull
    List<UserRoleItemDto> userRoleItemDtos;
}
