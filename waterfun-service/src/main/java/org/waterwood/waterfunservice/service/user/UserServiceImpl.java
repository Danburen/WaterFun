package org.waterwood.waterfunservice.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservice.service.NotificationService;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.api.resp.user.UserPublicCardResp;
import org.waterwood.waterfunservice.api.response.UserPublicProfileResp;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.exception.SelfFollowIsNotAllowException;
import org.waterwood.waterfunservicecore.exception.notfound.UserAssociationDataNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.UserNotFoundException;
import org.waterwood.waterfunservicecore.exception.privacy.FollowNotAllowedException;
import org.waterwood.waterfunservicecore.exception.privacy.ProfileNotVisibleException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.UserLikeRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.*;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.audit.UserActivityLogService;
import org.waterwood.waterfunservicecore.services.user.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserFollowerRepository userFollowerRepository;
    private final UserRepository userRepository;
    private final UserBriefService userBriefService;
    private final UserProfileRepository userProfileRepository;
    private final UserCounterRepository userCounterRepository;
    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;
    private final UserActivityLogService userActivityLogService;
    private final UserLikeRepository userLikeRepository;

    @Override
    public UserPublicProfileResp getPublicUserProfile(long userUid) {
        checkProfileVisibility(userUid);

        User u = userRepository.findById(userUid)
                .orElseThrow(() -> new UserNotFoundException(userUid));
        UserBrief ub = userBriefService.getUserBrief(userUid);
        UserProfile up = userProfileRepository.findByUserUid(userUid)
                .orElseThrow(() -> new UserAssociationDataNotFoundException(userUid, "UserProfile"));
        UserCounter uc = userCounterRepository.findByUserUid(userUid)
                .orElseThrow(() -> new UserAssociationDataNotFoundException(userUid, "UserCounter"));
        return new UserPublicProfileResp(
                        userUid,
                        ub,
                        up.getBio(),
                        up.getGender(),
                        up.getBirthDate(),
                        up.getResidence(),
                        u.getCreatedAt(),
                        uc.getFollowerCnt(),
                        uc.getFollowingCnt(),
                        uc.getPostCnt(),
                        uc.getLikeCnt()
                );
    }

    @Override
    public UserPublicCardResp getPublicUserCard(long userUid) {
        checkProfileVisibility(userUid);

        User u = userRepository.findById(userUid)
                .orElseThrow(() -> new UserNotFoundException(userUid));
        UserBrief ub = userBriefService.getUserBrief(userUid);
        UserCounter uc = userCounterRepository.findByUserUid(userUid)
                .orElseThrow(() -> new UserAssociationDataNotFoundException(userUid, "UserCounter"));
        return new UserPublicCardResp(
                u.getUid(),
                ub,
                uc.getFollowerCnt(),
                uc.getFollowingCnt(),
                uc.getLikeCnt(),
                uc.getPostCnt()
        );
    }

    @Override
    public Page<UserBrief> listUserFollowers(long userUid, Pageable pageable) {
        Page<Long> idPage = userFollowerRepository.findByUserUid(userUid, pageable);
        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        return new PageImpl<>(userBriefService.listUseBriefs(ids), pageable, idPage.getTotalElements());
    }

    @Override
    public Page<UserBrief> listUserFollowing(long userUid, Pageable pageable) {
        Page<Long> idPage = userFollowerRepository.findByFollowerUid(userUid, pageable);
        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        };
        return new PageImpl<>(userBriefService.listUseBriefs(ids), pageable, idPage.getTotalElements());
    }

    @Transactional
    @Override
    public void follow(long targetUid) {
        Long userUid = UserCtxHolder.getUserUid();
        if (userUid.equals(targetUid)) throw new SelfFollowIsNotAllowException();

        userSettingRepository.findById(targetUid).ifPresent(setting -> {
            if (Boolean.FALSE.equals(setting.getAllowFollow())) {
                throw new FollowNotAllowedException();
            }
        });

        userFollowerRepository.findById(new UserFollowerId(targetUid, userUid))
                .ifPresentOrElse(
                        uf -> {
                            userFollowerRepository.delete(uf);
                            userCounterRepository.decreaseUserFollowerCount(targetUid, 1);
                            userCounterRepository.decreaseUserFollowingCount(userUid, 1);
                            userActivityLogService.record(userUid, UserActionType.DELETED, BusinessType.NONE, targetUid);
                        },
                        () -> {
                            UserFollower uf = new UserFollower();
                            UserFollowerId ufid = new UserFollowerId(targetUid, userUid);
                            uf.setId(ufid);
                            userFollowerRepository.save(uf);
                            userCounterRepository.increaseUserFollowerCount(targetUid, 1);
                            userCounterRepository.increaseUserFollowingCount(userUid, 1);
                            notificationService.onNewFollower(targetUid, userUid);
                            userActivityLogService.record(userUid, UserActionType.CREATE, BusinessType.NONE, targetUid);
                        }
                );
    }

    @Override
    public Page<Long> getLikedPostIds(long userUid, Pageable pageable) {
        return userLikeRepository.findPostIdsByUserId(userUid, pageable);
    }

    private void checkProfileVisibility(long targetUid) {
        Long viewerUid = UserCtxHolder.getUserUid();
        // Always visible to self
        if (viewerUid != null && viewerUid.equals(targetUid)) return;

        userSettingRepository.findById(targetUid).ifPresent(setting -> {
            if (setting.getProfileVisibility() == ProfileVisibility.PRIVATE) {
                throw new ProfileNotVisibleException();
            }
            if (setting.getProfileVisibility() == ProfileVisibility.FOLLOWERS) {
                boolean isFollowing = viewerUid != null &&
                        userFollowerRepository.existsById(new UserFollowerId(targetUid, viewerUid));
                if (!isFollowing) {
                    throw new ProfileNotVisibleException();
                }
            }
        });
    }
}
