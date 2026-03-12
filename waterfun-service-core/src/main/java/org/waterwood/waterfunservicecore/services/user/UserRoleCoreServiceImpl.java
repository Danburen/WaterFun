package org.waterwood.waterfunservicecore.services.user;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserRoleCoreServiceImpl implements UserRoleCoreService{
    private final UserRoleRepo userRoleRepo;

    @Override
    public Set<UserRole> getUserRoles(long uid) {
        return userRoleRepo.findByUserUid(uid);
    }
}
