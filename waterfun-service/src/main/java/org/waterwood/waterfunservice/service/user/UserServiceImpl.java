package org.waterwood.waterfunservice.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.api.resp.user.UserPublicCardResp;
import org.waterwood.waterfunservice.api.response.UserPublicProfileResp;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.exception.SelfFollowIsNotAllowException;
import org.waterwood.waterfunservicecore.exception.notfound.UserAssociationDataNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.UserNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserCounterRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserFollowerRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
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

    @Override
    public UserPublicProfileResp getPublicUserProfile(long userUid) {
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

    @Override
    public void follow(long targetUid) {
        Long userUid = UserCtxHolder.getUserUid();
        if(userUid.equals(targetUid)) throw new SelfFollowIsNotAllowException();
        userFollowerRepository.findById(new UserFollowerId(targetUid, userUid)) // userUic -> targetUid
                .ifPresentOrElse(
                        uf -> {
                            userFollowerRepository.delete(uf);
                            userCounterRepository.decreaseUserFollowerCount(targetUid, 1);
                            userCounterRepository.decreaseUserFollowingCount(userUid, 1);
                        },
                        () -> {
                            UserFollower uf = new UserFollower();
                            UserFollowerId ufid = new UserFollowerId(targetUid, userUid);
                            uf.setId(ufid);
                            userFollowerRepository.save(uf);
                            userCounterRepository.increaseUserFollowerCount(targetUid, 1);
                            userCounterRepository.increaseUserFollowingCount(userUid, 1);
                        }
                );
    }


}
