package org.waterwood.waterfunadminservice.service.user.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.AuthContextHelper;
import org.waterwood.waterfunadminservice.service.user.UserProfileService;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepo upRepo;
    private final UserRepository userRepository;

    public UserProfileServiceImpl(UserProfileRepo upRepo,  UserRepository userRepository) {
        this.upRepo = upRepo;
        this.userRepository = userRepository;
    }

    @Override
    public void addUserProfile(UserProfile up) {
        upRepo.save(up);
    }

    @Override
    public void updateProfile(UserProfile profile) {
        User u = userRepository.findUserById(AuthContextHelper.getCurrentUserId()).orElseThrow(
                ()-> new AuthException(BaseResponseCode.USER_NOT_FOUND)
        );
        profile.setUser(u);
        upRepo.save(profile);
    }

    @Override
    public UserProfile getUserProfile(Long userId) {
        return upRepo.findUserProfileByUserId(userId).orElseThrow(
                ()-> new AuthException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public UserProfile getUserProfile() {
        return upRepo.findUserProfileByUserId(AuthContextHelper.getCurrentUserId()).orElseThrow(()-> new AuthException(BaseResponseCode.USER_NOT_FOUND));
    }

    @Transactional
    @Override
    public void updateAvatar(String avatarUrl) {
        upRepo.updateAvatarUrl( AuthContextHelper.getCurrentUserId(), avatarUrl);
    }
}
