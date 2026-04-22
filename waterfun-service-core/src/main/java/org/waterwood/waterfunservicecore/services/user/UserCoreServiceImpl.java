package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.RolePermission;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RolePermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.UserPermSpec;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.UserSpec;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCoreServiceImpl implements UserCoreService {
    private final UserRepository userRepository;
    private final UserRoleRepo userRoleRepo;
    private final UserPermRepo userPermRepo;
    private final RoleRepo roleRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RolePermRepo rolePermRepo;
    private final PermissionRepo permissionRepo;

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                ()-> new BizException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public User getUserByUid(long uid) {
        return  userRepository.findById(uid).orElseThrow(
                ()-> new BizException(BaseResponseCode.USER_NOT_FOUND)
        );
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
    public Set<Permission> getUserPermissions(long userUid) {
        if(userUid != UserCtxHolder.getUserUid()){
           throw  new BizException(BaseResponseCode.HTTP_UNAUTHORIZED);
        }
        List<Role> roles = userRoleRepo.findByUserUid(userUid).stream().map(UserRole::getRole).toList();

        Set<Permission> rolePermission = roles.stream()
                .flatMap(role-> getRolePermissions(role.getId()).stream())
                .collect(Collectors.toSet());

        Set<Permission> userPermission = userPermRepo.findByUserUid(userUid).stream()
                .map(UserPermission::getPermission).collect(Collectors.toSet());
        HashSet<Permission> permissions = new HashSet<>();
        permissions.addAll(rolePermission);
        permissions.addAll(userPermission);
        return permissions;
    }

    @Override
    public Set<Role> getRoles(long userUid) {
        return userRoleRepo.findByUserUid(userUid).stream().map(UserRole::getRole).collect(Collectors.toSet());
    }

    @Override
    public User changePwd(long userUid, String newPwd) {
        User u = getUser(userUid);
        if(u.getPasswordHash() == null) return u;
        if(encoder.matches(newPwd, u.getPasswordHash())){
            throw new BizException(BaseResponseCode.PASSWORD_TWO_PASSWORD_MUST_DIFFERENT);
        }
        u.setPasswordHash(encoder.encode(newPwd));
        return userRepository.save(u);
    }

    @Override
    public User getUser(long userUid){
        return userRepository.findUserByUid(userUid)
                .orElseThrow(() -> new NotFoundException("User UID: " + userUid));
    }

    @Override
    public Page<Role> listRoles(long uid, String roleName, Integer roleParent, Pageable pageable) {
        Specification<UserRole> spec = UserRoleSpec.of(uid, roleName, roleParent);
        Page<UserRole> userRoles = userRoleRepo.findAll(spec, pageable);
        return userRoles.map(
                UserRole::getRole
        );
    }

    @Override
    public Page<Permission> listPermissions(long uid, String name, String code, String resource, PermissionType type, Integer parentId, Pageable pageable) {
        return userPermRepo.findAll(UserPermSpec.of(uid, name, code, resource, type, parentId), pageable)
                .map(UserPermission::getPermission);
    }

    @Override
    public Permission getUserPermission(long uid, int id) {
        if(! userRoleRepo.existsById(uid)){
            throw new BizException(BaseResponseCode.USER_NOT_FOUND);
        }
        if(! permissionRepo.existsById(id)){
            throw new BizException(BaseResponseCode.PERMISSION_NOT_FOUND);
        }
        return userPermRepo.findByUserUidAndPermissionId(uid, id)
                .orElseThrow(() -> new BizException(BaseResponseCode.USER_PERMISSION_NOT_FOUND))
                .getPermission();
    }

    @Override
    public Page<User> listUsers(String username, String nickname, String accountStatus, Instant createdStart, Instant createdEnd, Pageable pageable) {
        Specification<User> spec = UserSpec.of(username, nickname, accountStatus, createdStart, createdEnd);
        return userRepository.findAll(spec, pageable);
    }

    @Override
    public int updateAvatar(Long userUid, String avatar) {
        return userRepository.updateAvatar(userUid, avatar);
    }

    @Override
    public String getUserAvatar(Long userUid) {
        return userRepository.getUserAvatarByUid(userUid);
    }

    private List<Permission> getRolePermissions(int roleId){
        return roleRepo.findById(roleId).map(role ->
                        rolePermRepo.findByRole(role).stream()
                                .map(RolePermission::getPermission)
                                .toList())
                .orElse(List.of());
    }
}
