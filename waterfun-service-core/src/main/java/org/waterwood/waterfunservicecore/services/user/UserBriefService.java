package org.waterwood.waterfunservicecore.services.user;

import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;

import java.util.List;
import java.util.Map;

public interface UserBriefService {
    /**
     * Query and cached user brief
     * @param userUids target uids
     * @return list of userbrief
     */
    List<UserBrief> listUseBriefs(List<Long> userUids);

    /**
     * Query and chache user brief
     * @param userUids targer user uids
     * @return map of user uid to {@link UserBrief} map
     */
    Map<Long, UserBrief> queryForMapUserIdBriefMap(List<Long> userUids);

    /**
     * Get singal user brief and cache it
     * @param userUid target user uid
     * @return {@link UserBrief} user brief entity
     */
    UserBrief getUserBrief(long userUid);

}
