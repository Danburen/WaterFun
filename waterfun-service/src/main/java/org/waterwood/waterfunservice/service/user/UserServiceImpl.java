package org.waterwood.waterfunservice.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservice.api.response.UserPublicCardResp;
import org.waterwood.waterfunservice.api.response.UserPublicInfoResp;
import org.waterwood.waterfunservice.api.response.UserPublicProfileResp;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserFollowerRepository;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.waterfunservicecore.services.user.UserCounterCoreService;
import org.waterwood.waterfunservicecore.services.user.UserDatumCoreService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserDatumCoreService userDatumCoreService;
    private final UserProfileCoreService userProfileCoreService;
    private final UserCounterCoreService userCounterCoreService;
    private final UserCoreService userCoreService;
    private final UserFollowerRepository userFollowerRepository;

    @Override
    public UserPublicInfoResp getPublicUserInfo(long userUid) {
        User u = userCoreService.getUser(userUid);
        if(! userCounterCoreService.isVisible(userUid)){
            throw new BizException(
                    BaseResponseCode.NOT_FOUND
            );
        }
        return new UserPublicInfoResp(
                u.getUid(),
                u.getUsername(),
                userProfileCoreService.getUserAvatar(userUid),
                u.getCreatedAt(),
                u.getLastActiveAt()
        );
    }

    @Override
    public UserPublicProfileResp getPublicUserProfile(long userUid) {
        User u = userCoreService.getUser(userUid);
        UserProfile up = userProfileCoreService.getUserProfile(userUid);
        UserCounter uc = userCounterCoreService.getUserCounter(userUid);
        if(!(uc.getVisible() == 1)) throw new BizException(
                BaseResponseCode.NOT_FOUND
        );
        return new UserPublicProfileResp(
                        up.getGender(),
                        uc.getLevel(),
                        uc.getExp(),
                        uc.getFollowerCnt(),
                        uc.getFollowingCnt(),
                        uc.getPostCnt(),
                        uc.getLikeCnt(),
                        u.getCreatedAt()
                );
    }

    @Override
    public UserPublicCardResp getPublicUserCard(long userUid) {
        User u = userCoreService.getUser(userUid);
        UserProfile up = userProfileCoreService.getUserProfile(userUid);
        UserCounter uc = userCounterCoreService.getUserCounter(userUid);
        return uc.isVisible() ? new UserPublicCardResp(
                u.getUid(),
                u.getUsername(),
                u.getNickname(),
                uc.getLevel(),
                uc.getVisible()
        ) : new UserPublicCardResp(
                null,
                null,
                null,
                null,
                uc.getVisible()
        );
    }

    @Override
    public Page<UserPublicCardResp> listUserFollowers(long spec, Pageable pageable) {
        return userFollowerRepository.fetchFollowers(spec, pageable).map(
                uf -> uf.getCounter().isVisible() ? new UserPublicCardResp(
                        uf.getFollower().getUid(),
                        uf.getUser().getUsername(),
                        uf.getFollower().getUsername(),
                        uf.getCounter().getLevel(),
                        uf.getCounter().getVisible()
                ): new UserPublicCardResp());
    }

    @Override
    public Page<UserPublicCardResp> listUserFollowing(long userUid, Pageable pageable) {
        return userFollowerRepository.fetchFollowings(userUid, pageable).map(
                uf -> new UserPublicCardResp(
                        uf.getFollower().getUid(),
                        uf.getUser().getUsername(),
                        uf.getFollower().getUsername(),
                        uf.getCounter().getLevel(),
                        uf.getCounter().getVisible()
                )
        );
    }
}
