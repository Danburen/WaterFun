package org.waterwood.waterfunadminservice.service.auth;

import org.waterwood.waterfunadminservice.api.response.user.AdminUserInfoResponse;

/**
 * An interface for current user which is admin self.
 */
public interface UserService {
    /**
     * Get current user info response
     * @return admin info response.
     */
    AdminUserInfoResponse getCurrentAdminUserInfo();
}
