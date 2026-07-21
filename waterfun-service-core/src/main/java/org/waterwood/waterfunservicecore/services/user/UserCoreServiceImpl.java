package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.perm.PermissionType;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RolePermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.InappropriateContentException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.entity.EncryptionDataKey;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.spec.UserPermSpec;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.entity.spec.UserSpec;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.audit.AuditLogCoreService;
import org.waterwood.waterfunservicecore.services.audit.UserActivityLogService;
import org.waterwood.waterfunservicecore.services.content.TextFilterService;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserCoreServiceImpl implements UserCoreService {
    private final UserRepository userRepository;
    private final UserRoleRepo userRoleRepo;
    private final UserPermRepo userPermRepo;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final RoleRepo roleRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RolePermRepo rolePermRepo;
    private final PermissionRepo permissionRepo;
    private final UserRoleCoreService userRoleCoreService;
    private final ResourceRepository resourceRepository;
    private final AuditLogCoreService auditLogCoreService;
    private final UserActivityLogService userActivityLogService;
    private final TextFilterService textFilterService;

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
    @Transactional
    public User changePwd(long userUid, String newPwd) {
        User u = getUser(userUid);
        // Allow setting password for users who registered without one (e.g., via SMS code)
        if(u.getPasswordHash() != null && encoder.matches(newPwd, u.getPasswordHash())){
            throw new BizException(BaseResponseCode.PASSWORD_TWO_PASSWORD_MUST_DIFFERENT);
        }
        u.setPasswordHash(encoder.encode(newPwd));
        User saved = userRepository.save(u);
        auditLogCoreService.recordSuccess(userUid, saved.getUsername(), AuditLogActionType.CHANGE_PASSWORD);
        return saved;
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
    public Page<User> listUsers(String username, String nickname, AccountStatus accountStatus, Instant createdStart, Instant createdEnd, Pageable pageable) {
        Specification<User> spec = UserSpec.of(username, nickname, accountStatus, createdStart, createdEnd);
        return userRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public int updateAvatarResourceUuid(Long userUid, String uuid) {
        int updated = userRepository.updateAvatarResourceByUid(
                uuid == null ? null : resourceRepository.getReferenceByUuid(uuid),
                userUid
        );
        // TODO: USUALLY SYSTEM CALLBACK UPDATE THIS
//        if (updated > 0) {
//            userActivityLogService.record(userUid, UserActionType.UPDATED, BusinessType.USER, null);
//        }
        return updated;
    }

    @Override
    @Transactional
    public void updateNickname(long userUid, String nickname) {
        User user = userRepository.findById(userUid)
                .orElseThrow(() -> new NotFoundException("User not found: " + userUid));
        if (textFilterService.containsSensitiveWords(nickname)) {
            throw new InappropriateContentException();
        }
        user.setNickname(nickname);
        userRepository.save(user);
    }

    @Override
    public boolean isUserAdmin(Long userUid) {
        return userRoleCoreService.getUserRoles(userUid).stream()
                .anyMatch(role -> role.getCode().equalsIgnoreCase(
                        userRoleCoreService.getAdminRoleCode()
                ));
    }

    @Override
    public Long resolveUid(String identifier) {
        // 1. Try phone (HMAC hash)
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
        String phoneHash = HashUtil.toSha256HmacString(identifier, hmacKey.getEncryptedKey());
        UserDatum ud = userDatumRepo.findByPhoneHash(phoneHash).orElse(null);
        if (ud != null) return ud.getUid();

        // 2. Try email (HMAC hash)
        String emailHash = HashUtil.toSha256HmacString(identifier, hmacKey.getEncryptedKey());
        ud = userDatumRepo.findByEmailHash(emailHash).orElse(null);
        if (ud != null) return ud.getUid();

        // 3. Try username
        User u = userRepository.findByUsername(identifier).orElse(null);
        if (u != null) return u.getUid();

        throw new NotFoundException("User not found for identifier: " + identifier);
    }

    private List<Permission> getRolePermissions(int roleId){
        return roleRepo.findById(roleId).map(role ->
                        rolePermRepo.findByRole(role).stream()
                                .map(RolePermission::getPermission)
                                .toList())
                .orElse(List.of());
    }
}
