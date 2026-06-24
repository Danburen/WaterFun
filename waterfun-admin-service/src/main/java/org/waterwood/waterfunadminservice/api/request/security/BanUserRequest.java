package org.waterwood.waterfunadminservice.api.request.security;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;

@Data
public class BanUserRequest {
    @NotNull
    private Long userUid;

    @NotNull
    private PenaltyType penaltyType;

    private BanReasonType banReasonType;
    private Long penaltyDurationHours;
    private String reasonText;
}
