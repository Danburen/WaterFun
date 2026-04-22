package org.waterwood.waterfunservicecore.services.user;

import org.waterwood.waterfunservicecore.entity.user.UserPermission;

import java.util.Set;

public interface UserPermissionCoreService {
    /**
     * Get the entity ofPending userPermission by a user uid
     * @param uid target user uid
     * @return set ofPending userPermissions
     */
    Set<UserPermission> getUserPermission(long uid);
}
