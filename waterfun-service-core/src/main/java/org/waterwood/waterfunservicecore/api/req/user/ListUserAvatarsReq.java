package org.waterwood.waterfunservicecore.api.req.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ListUserAvatarsReq {
    @NotNull
    private List<Long> userUids;
}
