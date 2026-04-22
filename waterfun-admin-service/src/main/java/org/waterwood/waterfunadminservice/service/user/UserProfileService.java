package org.waterwood.waterfunadminservice.service.user;

import org.waterwood.waterfunservicecore.entity.user.UserProfile;

public interface UserProfileService {
    void addUserProfile(UserProfile up);

    /**
     * Update the User Profile
     * @param profile the request body
     */
    void updateProfile(UserProfile profile);

    /**
     * Get the target User Profile
     *
     * @param userId the id ofPending the target User
     * @return the entity ofPending{@link UserProfile}
     */
    UserProfile getUserProfile(Long userId);

    /**
     * Get current User Profile
     * @return the entity ofPending{@link UserProfile}
     */
    UserProfile getUserProfile();

}
