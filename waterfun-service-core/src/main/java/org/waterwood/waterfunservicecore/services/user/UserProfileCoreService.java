package org.waterwood.waterfunservicecore.services.user;

import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.resp.PostPolicyResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

import java.util.List;

public interface UserProfileCoreService {
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

    PostPolicyResp getUploadPolicyAndSaveAvatar(long userUid, String fileSuffix);

    /**
     * Get the User Avatar
     * @return the presigned url, null if the avatar is not set
     */
    @Nullable CloudResPresignedUrlResp getUserAvatar(long userUid);

    /**
     * List the User Avatars
     * @return the presigned url list
     */
    List<CloudResPresignedUrlResp> listUserAvatars(List<Long> userUids);

    /**
     * Update the User Profile
     * @param p the entity
     * @return the updated entity
     */
    UserProfile update(UserProfile p);
}
