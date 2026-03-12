package org.waterwood.waterfunservicecore.services.user;

import org.waterwood.waterfunservicecore.entity.user.UserRole;

import java.util.Set;

public interface UserRoleCoreService {
    /**
     * Get the entity of userRole by a user uid
     * @param uid target user uid
     * @return set of userRoles
     */
    Set <UserRole> getUserRoles(long uid);
}
