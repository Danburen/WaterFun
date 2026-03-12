package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.entity.user.UserCounter;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserCounterRepository;

@Service
@RequiredArgsConstructor
public class UserCounterCoreServiceImpl implements UserCounterCoreService {
    private final UserCounterRepository userCounterRepository;

    @Override
    public UserCounter getUserCounter(long userUid) {
        return userCounterRepository.findById(userUid)
                .orElseThrow(
                        () -> new BizException(BaseResponseCode.USER_NOT_FOUND)
                );
    }

    @Override
    public boolean isVisible(long userUid) {
        return getUserCounter(userUid).getVisible() == 1;
    }
}
