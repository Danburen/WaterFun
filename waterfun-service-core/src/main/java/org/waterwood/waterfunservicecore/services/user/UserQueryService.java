package org.waterwood.waterfunservicecore.services.user;

import org.waterwood.waterfunservicecore.entity.user.UserBriefDO;

import java.util.List;

public interface UserQueryService {
    /**
     * Batch query and cache for user brief do list
     * @param userUids target user uids list
     * @return {@link UserBriefDO} list of DO for user brief
     */
    List<UserBriefDO> listBriefDOs(List<Long> userUids);

    /**
     * Query and cache for single user brief do
     * @param userUid target user do
     * @return {@link UserBriefDO} DO for user brief
     */
    UserBriefDO queryForSingalUserBriefDO(Long userUid);
}
