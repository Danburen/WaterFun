package org.waterwood.waterfunservicecore.infrastructure.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.time.Instant;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BanCheckAspect {

    private final UserPermRepo userPermRepo;

    @Before("@annotation(banCheck)")
    @Transactional(readOnly = true)
    public void checkBan(BanCheck banCheck) {
        long userUid = UserCtxHolder.getUserUid();
        String requiredBan = banCheck.value();

        for (UserPermission up : userPermRepo.findByUserUid(userUid)) {
            if (up.getPermission() == null || up.getPermission().getCode() == null) continue;
            if (up.getExpiresAt() != null && up.getExpiresAt().isBefore(Instant.now())) continue;
            if (!matchesBan(up.getPermission().getCode(), requiredBan)) continue;

            BanReasonType reason = up.getBanReasonType() != null ? up.getBanReasonType() : BanReasonType.UNSPECIFIED;
            String messageKey = reason.getMessageKey() != null ? reason.getMessageKey() : "http.forbidden";
            log.warn("Ban check failed: uid={}, perm={}, reason={}",
                    userUid, up.getPermission().getCode(), reason);
            throw new BizException(BaseResponseCode.HTTP_FORBIDDEN.getCode(), messageKey);
        }
    }

    private boolean matchesBan(String permCode, String requiredBan) {
        if (requiredBan == null || requiredBan.isBlank()) {
            return permCode.startsWith("ban:");
        }
        return permCode.equals(requiredBan);
    }
}
