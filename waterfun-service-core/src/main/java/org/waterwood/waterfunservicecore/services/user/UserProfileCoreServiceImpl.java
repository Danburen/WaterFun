package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.CloudStorageRootKey;
import org.waterwood.common.KeyConstants;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.utils.PathUtil;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserCoreMapper;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserProfileCoreMapper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudResOperationType;

import java.util.List;

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

    @Transactional
    @Override
    public PresignedResp getUploadPolicyAndSaveAvatar(long userUid, String fileSuffix) {
        if(fileSuffix == null){
            throw new BizException(BaseResponseCode.NEED_FILE_TYPE);
        }
        String avatarPath = PathUtil.getUniquePathFile(fileSuffix);
        User u = userCoreService.getUser(userUid);
        cloudFileService.removeFile( CloudStorageRootKey.UPLOADS,
                PathUtil.buildPath(KeyConstants.AVATAR, u.getAvatar()));
        // clean the cache
        redisHelper.del(
                cloudFileService.getCachedRedisKey(
                        userUid,
                        MediaResourceType.USER_AVATAR,
                        CloudResOperationType.WRITE
                )
        );

        u.setAvatar(avatarPath);
        userRepository.save(u);
        return cloudFileService.buildPutPolicyWithBiz(CloudStorageRootKey.UPLOADS, PathUtil.buildPath(
                KeyConstants.AVATAR,
                avatarPath
        ), String.valueOf(userUid));
    }

    @Override
    public @Nullable CloudResPresignedUrlResp getUserAvatar(long userUid) {
        User u = userCoreService.getUser(userUid);
        if(u.getAvatar() == null){
            return null;
        }
        return cloudFileService.getReadUrlCached(CloudStorageRootKey.UPLOADS,
                PathUtil.buildPath(KeyConstants.AVATAR, u.getAvatar()),
                String.valueOf(userUid), MediaResourceType.USER_AVATAR);
    }

    @Override
    public List<CloudResPresignedUrlResp> listUserAvatars(List<Long> userUids) {
        List<User> users = userRepository.findAllVisibleUsersByIds(userUids);
        List<String> paths = users.stream().map(
                User::getAvatar
        ).toList();
        List<String> bizIds = users.stream().map(
                u -> String.valueOf(u.getUid())
        ).toList();
        return cloudFileService.batchGetReadPublicUrlCached(
                paths,
                bizIds,
                MediaResourceType.USER_AVATAR
        );
    }

    @Override
    public UserProfile update(UserProfile p) {
        return userProfileRepo.save(p);
    }
}
