package org.waterwood.waterfunadminservice.api.request.user;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class UserRoleItemDto implements Serializable {
    @NotNull
    private int roleId;
    @Future
    private LocalDate expiresAt;
}
