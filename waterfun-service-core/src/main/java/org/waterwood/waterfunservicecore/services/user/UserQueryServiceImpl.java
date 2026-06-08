package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.user.UserBriefDO;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;

    @Cacheable(cacheNames = "userBriefDOs", key = "T(String).valueOf(#userUids.hashCode())")
    @Override
    public List<UserBriefDO> listBriefDOs(List<Long> userUids) {
        return userRepository.findBriefDOsByUidIn(userUids);
    }
    @Cacheable(cacheNames = "userBriefDOs", key = "T(String).valueOf(#userUid.hashCode())")
    @Override
    public UserBriefDO queryForSingalUserBriefDO(Long userUid) {
        return userRepository.findBriefDOsByUid(userUid);
    }
}
