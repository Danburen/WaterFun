package org.waterwood.waterfunadminservice.service.ticket;

import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;

import java.time.Instant;

public interface PenaltyService {

    void applyPenalty(Long userUid, PenaltyType penaltyType, BanReasonType reason, Instant expiresAt,
                      String targetId, TargetType targetType, String reasonText);

    void liftPenalty(Long userUid, PenaltyType penaltyType);

    void liftAllPenalties(Long userUid);
}
