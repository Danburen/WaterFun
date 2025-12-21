package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.utils.PathUtil;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.PostPolicyDto;
import org.waterwood.waterfunservicecore.api.resp.CloudResourcePresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserMapper;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserProfileMapper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.utils.FilePathKey;

@Service
@RequiredArgsConstructor
public class UserCoreProfileServiceImpl implements UserCoreProfileService {
    private final UserProfileRepo upRepo;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;
    private final CloudFileService cloudFileService;
    private final UserMapper userMapper;
    private final UserCoreService userCoreService;
    @Override
    public void addUserProfile(UserProfile up) {
        upRepo.save(up);
    }

    @Override
    @Transactional
    public void updateProfileByDto(long userUid, UpdateUserProfileRequest dto) {
        User u = userCoreService.getUser(userUid);
        UserProfile profile = getUserProfile(userUid);

        userProfileMapper.toEntity(dto, profile);
        userMapper.toEntity(dto, u);

        upRepo.save(profile);
        userRepository.save(u);
    }

    @Override
    public UserProfile getUserProfile(Long userUid) {
        return upRepo.findUserProfileByUserUid(userUid).orElseThrow(
                ()-> new BusinessException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Transactional
    @Override
    public PostPolicyDto getUploadPolicyAndSaveAvatar(long userUid, String fileSuffix) {
        String pathWithSuffix = PathUtil.getUniquePathFile(fileSuffix);
        PostPolicyDto dto =  cloudFileService.buildImgUploadsPutPolicy("avatar/" + pathWithSuffix);
        UserProfile userProfile = getUserProfile(userUid);
        cloudFileService.removeFile(FilePathKey.UPLOAD_IMG_PATH + userProfile.getAvatarUrl());
        userProfile.setAvatarUrl("avatar/" + pathWithSuffix);
        upRepo.save(userProfile);
        return dto;
    }

    @Override
    public @Nullable CloudResourcePresignedUrlResp getUserAvatar(long userUid) {
        UserProfile up = getUserProfile(userUid);
        if(up.getAvatarUrl() == null){
            return null;
        }
        return cloudFileService.getFileUrlFromCloud(FilePathKey.UPLOAD_IMG_PATH + up.getAvatarUrl());
    }
}
