package org.waterwood.waterfunadminservice.api.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserPermReq implements Serializable {
    @NotNull
    private List<UserPermItemDto> userPermissions;
}

