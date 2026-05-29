package org.waterwood.waterfunservicecore.services.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.waterwood.waterfunservicecore.entity.user.UserRole;

import java.util.Set;

public interface UserRoleCoreService {
    /**
     * Get the entity ofPending userRole by a user uid
     * @param uid target user uid
     * @return set ofPending userRoles
     */
    Set <UserRole> getUserRoles(long uid);

    String getAdminRoleCode();
}
