package org.waterwood.waterfunadminservice.service.user.impl;

import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.exception.notfound.UserNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunadminservice.service.user.UserProfileService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository upRepo;
    private final UserRepository userRepository;

    public UserProfileServiceImpl(UserProfileRepository upRepo, UserRepository userRepository) {
        this.upRepo = upRepo;
        this.userRepository = userRepository;
    }

    @Override
    public void addUserProfile(UserProfile up) {
        upRepo.save(up);
    }

    @Override
    public void updateProfile(UserProfile profile) {
        User u = userRepository.findUserByUid(UserCtxHolder.getUserUid()).orElseThrow(
               UserNotFoundException::new
        );
        profile.setUser(u);
        upRepo.save(profile);
    }

    @Override
    public UserProfile getUserProfile(Long userUid) {
        return upRepo.findByUserUid(userUid).orElseThrow(
                UserNotFoundException::new
        );
    }

    @Override
    public UserProfile getUserProfile() {
        return upRepo.findByUserUid(UserCtxHolder
                .getUserUid()).orElseThrow(UserNotFoundException::new);
    }

}
