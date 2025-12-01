package org.waterwood.waterfunadminservice.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunadminservice.dto.request.user.UserPwdUpdateRequestBody;
import org.waterwood.waterfunadminservice.dto.request.user.UserRoleItemDto;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.user.AccountStatus;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.AuthContextHelper;
import org.waterwood.waterfunadminservice.service.role.RoleServiceImpl;
import org.waterwood.waterfunadminservice.service.user.UserService;

import java.time.Instant;
import java.util.HashSet;
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

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                ()-> new BusinessException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public User getUserById(long id) {
        return  userRepository.findById(id).orElseThrow(
                ()-> new BusinessException(BaseResponseCode.USER_NOT_FOUND)
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

    private boolean checkPassword(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }

    @Override
    public User addUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User update(User user){
        return addUser(user);
    }

    @Transactional
    @Override
    public void updatePwd(UserPwdUpdateRequestBody userPwdUpdateRequestBody) {
        String oldPwd = userPwdUpdateRequestBody.getOldPwd();
        String newPwd = userPwdUpdateRequestBody.getNewPwd();
        String confirmPwd = userPwdUpdateRequestBody.getConfirmPwd();
        if(oldPwd.equals(newPwd)) throw new BusinessException(BaseResponseCode.PASSWORD_TWO_PASSWORD_MUST_DIFFERENT);
        if(! newPwd.equals(confirmPwd)) throw new BusinessException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
        User u = userRepository.findUserById(AuthContextHelper.getCurrentUserId()).orElseThrow(
                ()-> new BusinessException(BaseResponseCode.USER_NOT_FOUND)
        );
        if(checkPassword(newPwd, u.getPasswordHash())) throw  new BusinessException(BaseResponseCode.PASSWORD_TWO_PASSWORD_MUST_DIFFERENT);
        if(!checkPassword(oldPwd, u.getPasswordHash())) throw new BusinessException(BaseResponseCode.USERNAME_OR_PASSWORD_INCORRECT);
        userRepository.updatePassword(u.getId(), encoder.encode(newPwd));
    }

    @Override
    public Set<Permission> getUserPermissions(long userId) {
        List<Role> roles = userRoleRepo.findByUserId(userId).stream().map(UserRole::getRole).toList();

        Set<Permission> rolePermission = roles.stream()
                .flatMap(role-> roleService.getPermissions(role.getId()).stream())
                .collect(Collectors.toSet());

        Set<Permission> userPermission = userPermRepo.findByUserId(userId).stream()
                .map(UserPermission::getPermission).collect(Collectors.toSet());
        HashSet<Permission> permissions = new HashSet<>();
        permissions.addAll(rolePermission);
        permissions.addAll(userPermission);
        return permissions;
    }

    @Override
    public Set<Role> getRoles(long userId) {
        return userRoleRepo.findByUserId(userId).stream().map(UserRole::getRole).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void assignRoles(long id, List<UserRoleItemDto> userRoleItemDtos) {
        User user = this.getUserById(id);
        userRoleRepo.saveAll(toUserRoles(user, userRoleItemDtos, true));
    }

    @Override
    public void replace(long id, List<UserRoleItemDto> replacements) {
        User user = this.getUserById(id);
        userRoleRepo.deleteByUserId(id);
        if(CollectionUtil.isEmpty(replacements)) return;
        userRoleRepo.saveAll(toUserRoles(user, replacements, false));
    }

    @Override
    public void change(long id, List<UserRoleItemDto> adds, List<Integer> deletePermIds) {
        User user = this.getUserById(id);
        if(! CollectionUtil.isEmpty(deletePermIds)){
            userRoleRepo.deleteByUserIdAndRoleIdIn(id, deletePermIds);
        }
        if(! CollectionUtil.isEmpty(adds)){
            userRoleRepo.saveAll(toUserRoles(user, adds, false));
        };
    }

    @Transactional
    protected boolean findUserAndUpdateStatus(long userId, AccountStatus status) {
        return findUserAndUpdate(userId, user -> {
            user.setAccountStatus(status);
            user.setStatusChangedAt(Instant.now());
            user.setStatusChangeReason("Status changed to " + status.name());
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
            throw new BusinessException(BaseResponseCode.ROLE_NOT_FOUND_WITH_ARGS, notFounds);
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
