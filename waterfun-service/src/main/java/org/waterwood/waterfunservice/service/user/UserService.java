package org.waterwood.waterfunservice.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunservice.api.response.UserPublicCardResp;
import org.waterwood.waterfunservice.api.response.UserPublicInfoResp;
import org.waterwood.waterfunservice.api.response.UserPublicProfileResp;

public interface UserService {
    /**
     * Get a user's public info.
     * @param userUid target uid
     * @return public info dto
     */
    UserPublicInfoResp getPublicUserInfo(long userUid);

    /**
     * Get a user's public profile.
     * @param userUid target uid
     * @return public profile dto
     */
    UserPublicProfileResp getPublicUserProfile(long userUid);

    /**
     * Get a user's public card.
     * @param userUid target uid
     * @return public card dto
     */
    UserPublicCardResp getPublicUserCard(long userUid);

    /**
     * Get a user's following list.
     * @param spec  specification ofPending user's following
     * @return public card dto list
     */
    Page<UserPublicCardResp> listUserFollowers(long spec, Pageable pageable);
    /**
     * Get a user's following list.
     * @param userUid  specification ofPending user's following
     * @return public card dto list
     */
    Page<UserPublicCardResp> listUserFollowing(long userUid, Pageable pageable);
}
