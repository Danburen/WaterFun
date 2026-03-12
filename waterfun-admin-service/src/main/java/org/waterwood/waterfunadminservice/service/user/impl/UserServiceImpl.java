package org.waterwood.waterfunadminservice.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunadminservice.api.request.user.UserDatumUpdateAReq;
import org.waterwood.waterfunadminservice.api.request.user.UserInfoAUpdateReq;
import org.waterwood.waterfunadminservice.api.request.user.UserProfileUpdateAReq;
import org.waterwood.waterfunadminservice.api.request.user.UserRoleItemDto;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminDetail;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserAdminMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserCounterMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserProfileMapper;
import org.waterwood.waterfunservicecore.api.ToDictOption;
import org.waterwood.waterfunservicecore.api.resp.AccountResp;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunadminservice.service.role.RoleServiceImpl;
import org.waterwood.waterfunadminservice.service.user.UserService;
import org.waterwood.waterfunservicecore.services.user.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserRoleRepo userRoleRepo;
    private final RoleServiceImpl roleService;
    private final UserPermRepo userPermRepo;
    private final RoleRepo roleRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserCoreService userCoreService;
    private final UserProfileCoreService userProfileCoreService;
    private final UserDatumCoreService userDatumCoreService;
    private final UserCounterCoreService userCounterCoreService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCounterMapper userCounterMapper;
    private final UserRoleCoreService userRoleCoreService;
    private final UserPermissionCoreService userPermissionCoreService;
    private final UserAdminMapper userAdminMapper;

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                ()-> new BizException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public User getUserById(long id) {
        return  userRepository.findById(id).orElseThrow(
                ()-> new BizException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public boolean activateUser(long id) {
        return findUserAndUpdateStatus(id, AccountStatus.ACTIVE);
    }

    @Override
    public boolean deactivateUser(long id) {
        return findUserAndUpdateStatus(id, AccountStatus.DEACTIVATED);
    }

    @Override
    public boolean suspendUser(long id) {
        return findUserAndUpdateStatus(id, AccountStatus.SUSPENDED);
    }

    @Override
    public boolean deleteUser(long id) {
        return findUserAndUpdateStatus(id, AccountStatus.DELETED);
    }

    @Override
    public boolean isUserExist(long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public User addUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User update(User user){
        return addUser(user);
    }

    @Override
    @Transactional
    public void assignRoles(long id, List<UserRoleItemDto> userRoleItemDtos) {
        User user = this.getUserById(id);
        userRoleRepo.saveAll(toUserRoles(user, userRoleItemDtos, true));
    }

    @Override
    public void replace(long Uid, List<UserRoleItemDto> replacements) {
        User user = this.getUserById(Uid);
        userRoleRepo.deleteByUserUid(Uid);
        if(CollectionUtil.isEmpty(replacements)) return;
        userRoleRepo.saveAll(toUserRoles(user, replacements, false));
    }

    @Override
    public void change(long id, List<UserRoleItemDto> adds, List<Integer> deletePermIds) {
        User user = this.getUserById(id);
        if(! CollectionUtil.isEmpty(deletePermIds)){
            userRoleRepo.deleteByUserUidAndRoleIdIn(id, deletePermIds);
        }
        if(! CollectionUtil.isEmpty(adds)){
            userRoleRepo.saveAll(toUserRoles(user, adds, false));
        };
    }

    @Override
    public UserAdminDetail getUserDetail(long uid) {
        User u = userCoreService.getUser(uid);
        UserProfile p = userProfileCoreService.getUserProfile(uid);
        AccountResp acc = userDatumCoreService.getAccountInfo(uid);
        UserCounter c = userCounterCoreService.getUserCounter(uid);
        Set<UserRole> urs = userRoleCoreService.getUserRoles(uid);
        Set<UserPermission> ups = userPermissionCoreService.getUserPermission(uid);

        return new UserAdminDetail(
                userMapper.toDto(u),
                userProfileMapper.toResponse(p),
                userCounterMapper.toDto(c),
                acc,
                urs.stream().map(ToDictOption::toDictOption).collect(Collectors.toSet()),
                ups.stream().map(ToDictOption::toDictOption).collect(Collectors.toSet())
        );
    }

    @Override
    public void updateUserInfo(long uid, UserInfoAUpdateReq body) {
        User u = userCoreService.getUserByUid(uid);;
        u = userAdminMapper.toEntity(body, u);
        userCoreService.update(u);
    }

    @Override
    public void updateUserProfile(long uid, UserProfileUpdateAReq body) {
        UserProfile p = userProfileCoreService.getUserProfile(uid);
        p = userProfileMapper.toEntity(body, p);
        userProfileCoreService.update(p);
    }

    @Override
    public void updateUserDatum(long uid, UserDatumUpdateAReq body) {
        // TODO: send sms & email verification notification to target user;
        // TODO: add audit log
        if(body.getPhone() != null){
            userDatumCoreService.saveNewPhone(uid, body.getPhone(), false);
        }
        if(body.getEmail() != null){
            userDatumCoreService.saveNewEmail(uid, body.getEmail(), false);
        }
    }

    @Transactional
    protected boolean findUserAndUpdateStatus(long userId, AccountStatus status) {
        return findUserAndUpdate(userId, user -> {
            user.setAccountStatus(status);
            user.setStatusChangedAt(Instant.now());
        });
    }

    private boolean findUserAndUpdate(long userId, Consumer<User> updater) {
        return userRepository.findById(userId).map(user -> {
            updater.accept(user);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    /**
     * Convert userRoleItemDtos to UserRoles Entities
     * @param user  user
     * @param items userRoleItemDtos
     * @param strict whether missing roles should be strict and throw errors
     * @return List of UserRoles
     */
    private List<UserRole> toUserRoles(User user, List<UserRoleItemDto> items, boolean strict){
        Set<Integer> roleIds = items.stream()
                .map(UserRoleItemDto::getRoleId)
                .collect(Collectors.toSet());
        List<Role> roleEntities = roleRepo.findAllById(roleIds);
        Map<Integer, Role> roleMap = roleEntities.stream()
                .collect(Collectors.toMap(Role::getId, r -> r));

        if(strict && roleIds.size() != roleEntities.size()){
            List<Integer> notFounds = roleIds.stream()
                    .filter(id -> !roleMap.containsKey(id))
                    .toList();
            throw new BizException(BaseResponseCode.ROLE_NOT_FOUND_WITH_ARGS, notFounds);
        }

        return items.stream()
                .filter(item -> roleMap.containsKey(item.getRoleId()))
                .map(item ->{
                    UserRole userRole = new UserRole();
                    userRole.setUser(user);
                    userRole.setRole(roleMap.get(item.getRoleId()));
                    userRole.setExpiresAt(item.getExpiresAt() == null ? null : Instant.from(item.getExpiresAt()));
                    return userRole;
                }).toList();
    }
}
