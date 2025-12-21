package org.waterwood.waterfunservicecore.services.user;

import org.waterwood.waterfunservicecore.entity.user.UserCounter;

public interface UserCounterCoreService {
    /**
     * Get the userCounter entity by userUid
     * @param userUid target user's uid
     * @return userCounter entity
     */
    UserCounter getUserCounter(long userUid);

    /**
     * Check if the user is visible
     * @param userUid target user's uid
     * @return true if the user is visible
     */
    boolean isVisible(long userUid);
}
