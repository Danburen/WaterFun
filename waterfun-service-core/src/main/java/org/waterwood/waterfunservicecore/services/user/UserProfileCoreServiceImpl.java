package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserCoreMapper;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserProfileCoreMapper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileCoreServiceImpl implements UserProfileCoreService {
    private final UserProfileRepo upRepo;
    private final UserRepository userRepository;
    private final UserProfileCoreMapper userProfileCoreMapper;
    private final CloudFileService cloudFileService;
    private final UserCoreMapper userCoreMapper;
    private final UserCoreService userCoreService;
    private final RedisHelper redisHelper;
    private final UserProfileRepo userProfileRepo;
    private final ResourceRepository resourceRepository;

    @Override
    public void addUserProfile(UserProfile up) {
        upRepo.save(up);
    }

    @Override
    @Transactional
    public void updateProfileByDto(UpdateUserProfileRequest dto) {
        //TODO
        long userUid = UserCtxHolder.getUserUid();
        User u = userCoreService.getUser(userUid);
        UserProfile profile = getUserProfile(userUid);

        userProfileCoreMapper.toEntity(dto, profile);
        userCoreMapper.toEntity(dto, u);

        upRepo.save(profile);
        userRepository.save(u);
    }

    @Override
    public UserProfile getUserProfile(Long userUid) {
        return upRepo.findUserProfileByUserUid(userUid).orElseThrow(
                ()-> new BizException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public @Nullable CloudResPresignedUrlResp getUserAvatar(long userUid) {
        User u = userCoreService.getUser(userUid);
        if(u.getAvatarResource() == null){
            return null;
        }
        return resourceRepository.getByUuid(u.getAvatarResource().getUuid()).map(
                res -> cloudFileService.getReadUrlCached(
                        CloudFSRoot.USER,
                        res.getResourceKey(),
                        String.valueOf(userUid),
                        TargetType.USER_AVATAR)
        ).orElse(null);

    }

    @Override
    public Map<Long, CloudResPresignedUrlResp> listUserAvatars(List<Long> userUids) {
        List<User> users = userRepository.findAllVisibleUsersByIds(userUids);
        List<String> paths = users.stream().map(
                u -> u.getAvatarResource().getUuid()
        ).toList();
        Map<Long, String> userIdCosPathMap = users.stream().collect(
                Collectors.toMap(
                        User::getUid,
                        u -> u.getAvatarResource().getUuid()
                )
        );
        return cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                userIdCosPathMap,
                TargetType.USER_AVATAR
        );
    }

    @Override
    public UserProfile update(UserProfile p) {
        return userProfileRepo.save(p);
    }
}
