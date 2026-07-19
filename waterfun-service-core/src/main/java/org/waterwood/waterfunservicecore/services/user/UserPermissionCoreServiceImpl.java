package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;

import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserPermissionCoreServiceImpl implements UserPermissionCoreService{

    private final UserPermRepo userPermRepo;


    @Override
    public Set<UserPermission> getUserPermission(long uid) {
        return userPermRepo.findByUserUid(uid);
    }
}
