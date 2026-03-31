package org.waterwood.waterfunadminservice.service.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.TO.BatchResult;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunadminservice.api.request.role.*;
import org.waterwood.waterfunadminservice.infrastructure.exception.RoleException;
import org.waterwood.waterfunadminservice.infrastructure.mapper.RoleMapper;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.RolePermission;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RolePermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepo roleRepo;
    private final RolePermRepo rolePermRepo;
    private final PermissionRepo permissionRepo;
    private final RoleMapper roleMapper;
    private final IdentifierGenerator identifierGenerator;
    private final UserRoleRepo userRoleRepo;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    @Override
    public List<Permission> getPermissions(int roleId){
        return roleRepo.findById(roleId).map(role ->
                        rolePermRepo.findByRole(role).stream()
                        .map(RolePermission::getPermission)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public Page<Role> listRoles(Specification<Role> spec, Pageable pageable) {
        return roleRepo.findAll(spec, pageable);
    }

    @Override
    public Role getRole(int id) {
        return roleRepo.findById(id)
                .orElseThrow(()-> new RoleException(BaseResponseCode.ROLE_NOT_FOUND_WITH_ARGS, id));
    }

    @Override
    public Role addRole(CreateRoleRequest body) {
        Role role = new Role();
        role.setName(body.getName());
        role.setDescription(body.getDescription());
        role.setParent(body.getParentId() == null ? null :
                getRole(body.getParentId()));
        role.setDescription(body.getDescription());
        role.setCode(identifierGenerator.fromCode(body.getCode(), body.getName(), roleRepo));
        return roleRepo.save(role);
    }

    @Override
    public Role fullUpdateRole(int id, UpdateRoleRequest req) {
        Role role = getRole(id);
        updateRole(role, req.getParentId(), req.getCode(), req.getName());
        roleMapper.fullUpdate(req, role);
        return roleRepo.save(role);
    }

    @Override
    public List<Permission> listRolePerms(int id) {
        return rolePermRepo.findAllById(id)
                .stream()
                .map(RolePermission::getPermission)
                .toList();
    }

    @Override
    public BatchResult assignUsers(int id, List<Long> userIds, Instant expireAt) {
        Role role = getRole(id);
        int success = 0;
        if(CollectionUtil.isNotEmpty(userIds)){
            List<Long> distinctUserIds = userIds.stream().distinct().toList();
            List<User> users = userRepository.findAllById(distinctUserIds);
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUid, u -> u));

            List<UserRole> exists = userRoleRepo.findByRoleIdAndUserUidIn(id, new ArrayList<>(userMap.keySet()));
            Map<Long, UserRole> existsMap = exists.stream()
                    .collect(Collectors.toMap(ur -> ur.getUser().getUid(), ur -> ur));

            List<UserRole> updates = new ArrayList<>();
            List<UserRole> inserts = new ArrayList<>();
            for (Long uid : distinctUserIds) {
                User user = userMap.get(uid);
                if (user == null) {
                    continue;
                }
                UserRole existed = existsMap.get(uid);
                if (existed == null) {
                    UserRole userRole = new UserRole();
                    userRole.setUser(user);
                    userRole.setRole(role);
                    userRole.setExpiresAt(expireAt);
                    inserts.add(userRole);
                } else {
                    existed.setExpiresAt(expireAt);
                    updates.add(existed);
                }
            }
            if (CollectionUtil.isNotEmpty(updates)) {
                userRoleRepo.saveAll(updates);
            }
            if (CollectionUtil.isNotEmpty(inserts)) {
                userRoleRepo.saveAll(inserts);
            }
            success = updates.size() + inserts.size();
        }
        return BatchResult.of(userIds.size(), success);
    }

    @Override
    public Page<User> getRoleUsers(int id, Pageable pageable) {
        return userRoleRepo.findByRoleId(id, pageable)
                .map(UserRole::getUser);
    }

    @Override
    public BatchResult removeRoleUsers(int id, List<Long> removeRoleUserUids) {
        int removes = 0;
        if(CollectionUtil.isNotEmpty(removeRoleUserUids)){
            removes = userRoleRepo.deleteByRoleIdAndUserUids(id, removeRoleUserUids);
        }
        return BatchResult.of( removeRoleUserUids.size(), removes);
    }

    @Override
    public BatchResult removeRolePerms(int id, List<Integer> permIds) {
        int removes = 0;
        if(CollectionUtil.isNotEmpty(permIds)){
            removes = rolePermRepo.deleteByRoleIdAndPermissionIdIn(id, permIds);
        }
        return BatchResult.of(permIds.size(), removes);
    }

    @Override
    public BatchResult replaceUserRoles(int id, List<Long> userUids, Instant expiresAt) {
        int removes = userRoleRepo.deleteByRoleId(id);
        BatchResult replaces = assignUsers(id, userUids, expiresAt);
        return BatchResult.of(userUids.size(), replaces.getSuccess() + removes);
    }

    private void updateRole(Role role,@Nullable Integer parentId,@Nullable String code, @Nullable String name) {
        if(parentId != null) {
            if(parentId.equals(role.getId()))
                throw new RoleException(BaseResponseCode.PARENT_MUST_DIFFERENT, role.getId());
            Role parent = getRole(parentId);
            role.setParent(parent);
        }
        if(code != null){
            if(code.equals(role.getCode())) return;
            role.setCode(identifierGenerator.fromCode(code, name == null ? code : name, roleRepo));
        }
    }

    @Override
    public void deleteRole(int id) {
        roleRepo.deleteById(id);
    }

    @Override
    @Transactional
    public BatchResult assignPerms(int id, List<RolePermItemDTO> assignments) {
        Role role = this.getRole(id);
        List<RolePermission> toSaved = toRolePermissions(role, assignments);
        if (CollectionUtil.isNotEmpty(toSaved)) {
            List<Integer> permissionIds = toSaved.stream()
                    .map(rp -> rp.getPermission().getId())
                    .toList();

            List<RolePermission> existing = rolePermRepo.findByRoleIdAndPermissionIdIn(id, permissionIds);
            Map<Integer, RolePermission> existingMap = existing.stream()
                    .collect(Collectors.toMap(rp -> rp.getPermission().getId(), rp -> rp));

            List<RolePermission> newRecords = new ArrayList<>();
            List<RolePermission> updates = new ArrayList<>();
            for (RolePermission incoming : toSaved) {
                RolePermission existed = existingMap.get(incoming.getPermission().getId());
                if (existed == null) {
                    newRecords.add(incoming);
                    continue;
                }
                existed.setExpiresAt(incoming.getExpiresAt());
                updates.add(existed);
            }

            if (CollectionUtil.isNotEmpty(updates)) {
                rolePermRepo.saveAll(updates);
            }
            if (CollectionUtil.isNotEmpty(newRecords)) {
                rolePermRepo.saveAll(newRecords);
            }
        }
        return BatchResult.of(assignments.size(), toSaved.size());
    }

    @Override
    @Transactional
    public BatchResult replaceAllRolePerms(int id, List<RolePermItemDTO> replacements) {
        Role role = getRole(id);
        rolePermRepo.deleteByRoleId(id);
        List<RolePermission> toSaved = Collections.emptyList();
        if(CollectionUtil.isNotEmpty(replacements)) {
            toSaved = toRolePermissions(role, replacements);
        }
        rolePermRepo.saveAll(toSaved);
        return BatchResult.of(replacements.size(), replacements.size() - toSaved.size());
    }

    /**
     * convert RolePermItemDTO to RolePermission
     *
     * @param role  role
     * @param items items
     * @return role permissions
     */
    private List<RolePermission> toRolePermissions(Role role, List<RolePermItemDTO> items){
        if(CollectionUtil.isEmpty(items)){
            return Collections.emptyList();
        }
        List<RolePermItemDTO> distinctItems = items.stream().distinct().toList();
        log.info("Assigning permissions to role {}, total: {}, distinct: {}", role.getId(), items.size(), distinctItems.size());
        List<Permission> perms = permissionRepo.findAllById(
                distinctItems.stream()
                        .map(RolePermItemDTO::getPermissionId)
                        .toList()
        );
        Map<Integer, Permission> permMap = perms.stream()
                .collect(Collectors.toMap(Permission::getId, p -> p));

        return distinctItems.stream()
                .map(dto -> {
                    Permission permission = permMap.get(dto.getPermissionId());
                    if (permission == null) {
                        log.warn("Permission not found, skip assignment. roleId={}, permissionId={}", role.getId(), dto.getPermissionId());
                        return null;
                    }
                    RolePermission rolePerm = new RolePermission();
                    rolePerm.setRole(role);
                    rolePerm.setPermission(permission);
                    rolePerm.setExpiresAt(dto.getExpiresAt() == null ? null : Instant.from(dto.getExpiresAt()));
                    return rolePerm;
                })
                .filter(Objects::nonNull)
                .toList();
    }
}