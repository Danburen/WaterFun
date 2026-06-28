package org.waterwood.waterfunservice.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.api.resp.user.UserPublicCardResp;
import org.waterwood.waterfunservice.api.response.UserPublicProfileResp;

public interface UserService {

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
     *
     * @param spec specification ofPending user's following
     * @return public card dto list
     */
    Page<UserBrief> listUserFollowers(long spec, Pageable pageable);
    /**
     * Get a user's following list.
     *
     * @param userUid specification ofPending user's following
     * @return public card dto list
     */
    Page<UserBrief> listUserFollowing(long userUid, Pageable pageable);

    /**
     * User follow a user
     * @param uid target user uid
     */
    void follow(long uid);

    /**
     * Get paginated liked post IDs for a user
     * @param userUid target user uid
     * @param pageable pageable
     * @return page of post IDs
     */
    Page<Long> getLikedPostIds(long userUid, Pageable pageable);
}
