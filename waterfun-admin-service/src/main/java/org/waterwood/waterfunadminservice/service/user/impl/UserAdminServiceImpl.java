package org.waterwood.waterfunadminservice.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.waterfunadminservice.api.response.user.RiskLevel;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminBrief;
import org.waterwood.waterfunadminservice.infrastructure.exception.BuiltInResourceProtectedException;
import org.waterwood.waterfunadminservice.infrastructure.exception.PermissionReferenceInvalidException;
import org.waterwood.waterfunadminservice.infrastructure.exception.RoleReferenceInvalidException;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.utils.UidGenerator;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunadminservice.api.request.user.*;
import org.waterwood.waterfunadminservice.api.response.perm.AssignedPermissionRes;
import org.waterwood.waterfunadminservice.api.response.role.AssignedRoleRes;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminDetail;
import org.waterwood.waterfunadminservice.infrastructure.exception.UserAdminException;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserAdminMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserCounterMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserProfileMapper;
import org.waterwood.waterfunservicecore.api.resp.AccountResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.user.Role;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.exception.notfound.UserNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.UserReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.*;
import org.waterwood.waterfunadminservice.service.user.UserAdminService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {
    private final UserRepository userRepository;
    private final UserRoleRepo userRoleRepo;
    private final UserPermRepo userPermRepo;
    private final RoleRepo roleRepo;
    private final PermissionRepo permissionRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserCoreService userCoreService;
    private final UserProfileCoreService userProfileCoreService;
    private final UserDatumCoreService userDatumCoreService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCounterMapper userCounterMapper;
    private final UserRoleCoreService userRoleCoreService;
    private final UserPermissionCoreService userPermissionCoreService;
    private final UserAdminMapper userAdminMapper;
    private final EncryptedKeyService encryptedKeyService;
    private final UserDatumRepo userDatumRepo;
    private final UidGenerator uidGenerator;
    private final UserProfileRepository userProfileRepository;
    private final UserCounterRepository userCounterRepository;
    private final UserFollowerRepository userFollowerRepository;
    private final UserBriefService userBriefService;
    private final PostRepository postRepository;
    private final AuditTaskRepository auditTaskRepository;
    private final CloudFileService cloudFileService;

    @Override
    public User getUserById(long id) {
        return  userRepository.findById(id).orElseThrow(
                ()-> new UserNotFoundException(id)
        );
    }

    @Transactional
    @Override
    public void deleteUser(long uid) {
        // TODO: ADD AUDIT LOG
        User u = userRepository.findById(uid).orElseThrow(
                ()-> new UserNotFoundException(uid)
        );
        if(u.getUserType() == UserType.ADMIN){
            throw new BuiltInResourceProtectedException("ADMIN");
        }
        userRepository.deleteUserByUid(uid);
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
        List<UserRole> incoming = toUserRoles(user, userRoleItemDtos, true);
        if (CollectionUtil.isEmpty(incoming)) {
            return;
        }

        Set<Integer> roleIds = incoming.stream().map(ur -> ur.getRole().getId()).collect(Collectors.toSet());
        List<UserRole> existing = userRoleRepo.findByUserUidAndRoleIdIn(id, roleIds);
        Map<Integer, UserRole> existingMap = existing.stream()
                .collect(Collectors.toMap(ur -> ur.getRole().getId(), ur -> ur));

        List<UserRole> updates = new ArrayList<>();
        List<UserRole> inserts = new ArrayList<>();
        for (UserRole candidate : incoming) {
            int roleId = candidate.getRole().getId();
            UserRole existed = existingMap.get(roleId);
            if (existed == null) {
                inserts.add(candidate);
            } else {
                existed.setExpiresAt(candidate.getExpiresAt());
                updates.add(existed);
            }
        }

        if (CollectionUtil.isNotEmpty(updates)) {
            userRoleRepo.saveAll(updates);
        }
        if (CollectionUtil.isNotEmpty(inserts)) {
            userRoleRepo.saveAll(inserts);
        }
    }

    @Override
    public void replace(long Uid, List<UserRoleItemDto> replacements) {
        User user = this.getUserById(Uid);
        userRoleRepo.deleteByUserUid(Uid);
        if(CollectionUtil.isEmpty(replacements)) return;
        userRoleRepo.saveAll(toUserRoles(user, replacements, false));
    }

    @Override
    @Transactional
    public void assignPermissions(long id, List<UserPermItemDto> userPermItemDtos) {
        User user = this.getUserById(id);
        List<UserPermission> incoming = toUserPerms(user, userPermItemDtos, true);
        if (CollectionUtil.isEmpty(incoming)) {
            return;
        }

        Set<Integer> permissionIds = incoming.stream()
                .map(up -> up.getPermission().getId())
                .collect(Collectors.toSet());

        List<UserPermission> existing = userPermRepo.findByUserUidAndPermissionIdIn(id, permissionIds);
        Map<Integer, UserPermission> existingMap = existing.stream()
                .collect(Collectors.toMap(up -> up.getPermission().getId(), up -> up));

        List<UserPermission> updates = new ArrayList<>();
        List<UserPermission> inserts = new ArrayList<>();
        for (UserPermission candidate : incoming) {
            int permissionId = candidate.getPermission().getId();
            UserPermission existed = existingMap.get(permissionId);
            if (existed == null) {
                inserts.add(candidate);
            } else {
                existed.setExpiresAt(candidate.getExpiresAt());
                updates.add(existed);
            }
        }

        if (CollectionUtil.isNotEmpty(updates)) {
            userPermRepo.saveAll(updates);
        }
        if (CollectionUtil.isNotEmpty(inserts)) {
            userPermRepo.saveAll(inserts);
        }
    }

    @Override
    @Transactional
    public BatchResult removeRoles(long id, List<Integer> roleIds) {
        this.getUserById(id);
        if (CollectionUtil.isEmpty(roleIds)) {
            return BatchResult.empty();
        }
        Set<Integer> distinctRoleIds = roleIds.stream().collect(Collectors.toSet());
        int removed = userRoleRepo.deleteByUserUidAndRoleIdIn(id, distinctRoleIds);
        return BatchResult.of(roleIds.size(), removed);
    }

    @Override
    @Transactional
    public BatchResult removePermissions(long id, List<Integer> permissionIds) {
        this.getUserById(id);
        if (CollectionUtil.isEmpty(permissionIds)) {
            return BatchResult.empty();
        }
        Set<Integer> distinctPermissionIds = permissionIds.stream().collect(Collectors.toSet());
        int removed = userPermRepo.deleteByUserUidAndPermissionIdIn(id, distinctPermissionIds);
        return BatchResult.of(permissionIds.size(), removed);
    }

    @Override
    public Page<AssignedRoleRes> listAssignedRoles(long uid, Integer roleId, String code, String name, Pageable pageable) {
        Page<UserRole> res = userRoleRepo.listUserRoles(uid, roleId, code, name, pageable);
        return res.map(ur -> {
            AssignedRoleRes a = new AssignedRoleRes();
            a.setAssignedAt(ur.getCreatedAt());
            a.setExpiresAt(ur.getExpiresAt());
            Role r = ur.getRole();
            a.setId(r.getId());
            a.setName(r.getName());
            a.setCode(r.getCode());
            return a;
        });
    }

    @Override
    public Page<AssignedPermissionRes> listAssignedPermissions(long uid, Integer permId, String name, String code, Pageable pageable) {
        Page<UserPermission> res = userPermRepo.listUserPerms(uid, permId, name, code, pageable);
        return res.map(up -> {
            AssignedPermissionRes a = new AssignedPermissionRes();
            a.setAssignedAt(up.getCreatedAt());
            a.setExpiresAt(up.getExpiresAt());
            Permission p = up.getPermission();
            a.setId(p.getId());
            a.setName(p.getName());
            a.setCode(p.getCode());
            return a;
        });
    }

    @Transactional
    @Override
    public UserAdminDetail getUserDetail(long uid) {
        User u = userRepository.findById(uid).orElseThrow(
                UserNotFoundException::new
        );
        UserProfile p = userProfileRepository.findByUserUid(uid).orElseGet(() -> {;
            UserProfile up = new UserProfile();
            up.setUser(u);
            return userProfileRepository.save(up);
        });
        AccountResp acc = userDatumCoreService.getAccountInfo(uid);
        UserCounter c = userCounterRepository.findByUserUid(uid).orElseGet(() -> {
            UserCounter uc = new UserCounter();
            uc.setUser(u);
            return userCounterRepository.save(uc);
        });
        Set<UserRole> urs = userRoleCoreService.getUserRoles(uid);
        Set<UserPermission> ups = userPermissionCoreService.getUserPermission(uid);

        return new UserAdminDetail(
                userMapper.toUserInfoARes(u),
                userProfileMapper.toResponse(p),
                userCounterMapper.toDto(c),
                acc,
                urs.stream().map(ur-> ur.getRole().toOption().toExpirableOption(ur.getExpiresAt())).collect(Collectors.toSet()),
                ups.stream().map(up -> up.getPermission().toOption().toExpirableOption(up.getExpiresAt())).collect(Collectors.toSet())
        );
    }

    @Override
    public void updateUserInfo(long uid, UserInfoAUpdateReq body) {
        User u = userRepository.findById(uid).orElseThrow(
                () -> new UserNotFoundException(uid)
        );
        u = userAdminMapper.toEntity(body, u);
        userRepository.save(u);
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

    @Override
    public List<OptionVO<Long>> getAllUserOptions() {
        return userRepository.findAll().stream()
                .map(User::toOptionVO)
                .toList();
    }

    @Transactional
    @Override
    public User createUser(CreateNewUserReq req) {
        EncryptionDataKey key = encryptedKeyService.getAesKey();
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
        String phone = req.getPhone();
        String pwd = req.getPassword();
        String username = req.getUsername();

        userRepository.findByUsername(username).ifPresent(_ -> {
            throw new UserAdminException(BaseResponseCode.USER_ALREADY_EXISTS);
        });
        User user = new User();
        user.setUsername(username);
        user.setUid(uidGenerator.generateUid());
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setUserType(req.getUserType() != null ? UserType.fromValue(req.getUserType()) : UserType.COMMON);
        if(StringUtil.isNotBlank( req.getPassword())) user.setPasswordHash(encoder.encode(pwd));

        UserDatum ud = new UserDatum();
        ud.setUser(user);
        ud.setUid(user.getUid());
        ud.setEncryptionKeyId(key.getKeyId());

        if(StringUtil.isNotBlank(phone)){
            userDatumRepo.findByPhoneHash(HashUtil.Sha256HmacString(phone, hmacKey.getEncryptedKey())).ifPresent(
                    _->{
                        throw new UserAdminException(BaseResponseCode.PHONE_NUMBER_ALREADY_USED);
                    }
            );
            String encryptedPhone = EncryptionHelper.encryptField(phone, key);
            ud.setPhoneEncrypted(encryptedPhone);
            ud.setPhoneHash(HashUtil.Sha256HmacString(req.getPhone(), hmacKey.getEncryptedKey()));
        }

        ud.setPhoneVerified(true);
        UserProfile up = new UserProfile();
        up.setUser(user);
        UserCounter uc = new UserCounter();
        uc.setUser(user);

        user.setUserCounter(uc);
        user.setUserProfile(up);
        user.setUserDatum(ud);
        userRepository.save(user);
        return user;
    }

    @Override
    public BatchResult batchDeleteUsers(List<Long> userUids) {
        if (CollectionUtil.isEmpty(userUids)) {
            return BatchResult.empty();
        }
        // TODO: add audit log
        int deleted = userRepository.deleteUserByUidIn(userUids);
        return BatchResult.of(userUids.size(), deleted);
    }

    @Override
    public UserAdminBrief getUserAdminBrief(Long uid) {
        User u = userRepository.findById(uid).orElseThrow(
                () -> new UserReferenceInvalidException(uid)
        );
        Instant userCreatedTime = userRepository.getUserCreatedAtByUid(uid);
        long postCount = postRepository.countByAuthorUidAndIsDeleted(uid, false);
        long rejectedCount = auditTaskRepository.countBySubmitterUidAndStatus(uid, AuditStatus.REJECTED);
        UserCounter uc = userCounterRepository.findByUserUid(uid).orElseThrow(
                () -> new ServiceException("UserCounter not found for user " + uid)
        );
        String avatarUrlKey = u.getAvatarResource().getResourceKey();
        CloudResPresignedUrlResp avatarUrl = avatarUrlKey == null ? null : cloudFileService.getReadUrlCached(
                CloudFSRoot.UPLOADS,
                u.getAvatarResource().getResourceKey(),
                uid,
                TargetType.USER_AVATAR
        );
        RiskLevel riskLevel = RiskLevel.calculate(uc.getSubmitCnt(), uc.getRejectCnt(), uc.getReportCnt(), rejectedCount);
        return new UserAdminBrief(
                u.getUid(),u.getDisplayName(), avatarUrl,
                u.getLevel(), u.getUserType(),
                userCreatedTime, postCount, riskLevel
        );
    }

    @Override
    public Map<Long, UserAdminBrief> batchGetUserAdminBrief(List<Long> userUids) {
        List<User> users = userRepository.findAllByUidIn(userUids);
        Map<Long, CloudResPresignedUrlResp> userAvatarUrls = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                users.stream().collect(Collectors.toMap(
                        User::getUid,
                        u -> u.getAvatarResource().getResourceKey()
                )),
                TargetType.USER_AVATAR);
        Map<Long, UserCounter> userCounterMap = userCounterRepository.findAllByUserUidIn((userUids)).stream()
                .collect(Collectors.toMap(c-> c.getUser().getUid(), c -> c));
        Map<Long, RiskLevel> userRiskLevelMap = userCounterMap.values().stream().collect(Collectors.toMap(
                u -> u.getUser().getUid(),
                uc -> {
                    return RiskLevel.calculate(uc.getSubmitCnt(), uc.getRejectCnt(), uc.getReportCnt(), uc.getRejectCnt());
                }
        ));
        return users.stream().map(u -> {
            UserCounter counter = userCounterMap.get(u.getUid());
            RiskLevel riskLevel = userRiskLevelMap.get(u.getUid());
            CloudResPresignedUrlResp avatarUrl = userAvatarUrls.get(u.getUid());
            return new UserAdminBrief(
                    u.getUid(), u.getNickname() == null ? u.getUsername() : u.getNickname(), avatarUrl,
                    u.getLevel(), u.getUserType(), u.getCreatedAt(), counter != null ? counter.getPostCnt() : 0,
                    riskLevel != null ? riskLevel : RiskLevel.LOW
            );
        }).collect(
                Collectors.toMap(UserAdminBrief::getUid, b -> b)
        );
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
     * @return List ofPending UserRoles
     */
    private List<UserRole> toUserRoles(User user, List<UserRoleItemDto> items, boolean strict){
        if (CollectionUtil.isEmpty(items)) {
            return List.of();
        }

        Map<Integer, UserRoleItemDto> deduplicatedItems = items.stream()
                .collect(Collectors.toMap(
                        UserRoleItemDto::getRoleId,
                        item -> item,
                        (first, second) -> second,
                        LinkedHashMap::new
                ));

        Set<Integer> roleIds = deduplicatedItems.keySet();
        List<Role> roleEntities = roleRepo.findAllById(roleIds);
        Map<Integer, Role> roleMap = roleEntities.stream()
                .collect(Collectors.toMap(Role::getId, r -> r));

        if(strict && roleIds.size() != roleEntities.size()){
            List<Integer> notFounds = roleIds.stream()
                    .filter(id -> !roleMap.containsKey(id))
                    .toList();
            throw new RoleReferenceInvalidException(notFounds.toArray());
        }

        return deduplicatedItems.values().stream()
                .filter(item -> roleMap.containsKey(item.getRoleId()))
                .map(item ->{
                    UserRole userRole = new UserRole();
                    userRole.setUser(user);
                    userRole.setRole(roleMap.get(item.getRoleId()));
                    userRole.setExpiresAt(item.getExpiresAt() == null ? null : Instant.from(item.getExpiresAt()));
                    return userRole;
                }).toList();
    }

    private List<UserPermission> toUserPerms(User user, List<UserPermItemDto> items, boolean strict) {
        if (CollectionUtil.isEmpty(items)) {
            return List.of();
        }

        Map<Integer, UserPermItemDto> deduplicatedItems = items.stream()
                .collect(Collectors.toMap(
                        UserPermItemDto::getPermissionId,
                        item -> item,
                        (ignored, second) -> second,
                        LinkedHashMap::new
                ));

        Set<Integer> permissionIds = deduplicatedItems.keySet();
        List<Permission> permissionEntities = permissionRepo.findAllById(permissionIds);
        Map<Integer, Permission> permissionMap = permissionEntities.stream()
                .collect(Collectors.toMap(Permission::getId, p -> p));

        if (strict && permissionIds.size() != permissionEntities.size()) {
            List<Integer> notFounds = permissionIds.stream()
                    .filter(pid -> !permissionMap.containsKey(pid))
                    .toList();
            throw new PermissionReferenceInvalidException(notFounds.toString());
        }

        return deduplicatedItems.values().stream()
                .filter(item -> permissionMap.containsKey(item.getPermissionId()))
                .map(item -> {
                    UserPermission userPerm = new UserPermission();
                    userPerm.setUser(user);
                    userPerm.setPermission(permissionMap.get(item.getPermissionId()));
                    userPerm.setExpiresAt(item.getExpiresAt() == null ? null : Instant.from(item.getExpiresAt()));
                    return userPerm;
                })
                .toList();
    }
}
