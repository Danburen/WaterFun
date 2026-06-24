package org.waterwood.waterfunadminservice.api.request.security;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;

@Data
public class LiftPenaltyRequest {
    @NotNull
    private Long userUid;

    private PenaltyType penaltyType;
}
