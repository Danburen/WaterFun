package org.waterwood.waterfunservicecore.services.user;

import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.PostPolicyDto;
import org.waterwood.waterfunservicecore.api.resp.CloudResourcePresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

public interface UserCoreProfileService {
    void addUserProfile(UserProfile up);

    /**
     * Update the User Profile
     *
     * @param userUid
     * @param dto     the DTO
     */
    void updateProfileByDto(long userUid, UpdateUserProfileRequest dto);

    /**
     * Get the target User Profile
     *
     * @param userUid the id of the target User
     * @return the entity
     */
    UserProfile getUserProfile(Long userUid);

    PostPolicyDto getUploadPolicyAndSaveAvatar(long userUid, String fileSuffix);

    /**
     * Get the User Avatar
     * @return the presigned url, null if the avatar is not set
     */
    @Nullable CloudResourcePresignedUrlResp getUserAvatar(long userUid);
}
