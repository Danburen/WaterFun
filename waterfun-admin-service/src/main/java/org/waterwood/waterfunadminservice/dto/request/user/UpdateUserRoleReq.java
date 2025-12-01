package org.waterwood.waterfunadminservice.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class UpdateUserRoleReq implements Serializable {
    @NotNull
    private List<UserRoleItemDto> userRoleItemDtos;
}
