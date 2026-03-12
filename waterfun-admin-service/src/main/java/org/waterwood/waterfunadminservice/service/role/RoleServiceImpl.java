package org.waterwood.waterfunadminservice.service.role;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunadminservice.api.request.role.RolePermItemDTO;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.RolePermission;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RolePermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepo roleRepo;
    private final RolePermRepo rolePermRepo;
    private final PermissionRepo permissionRepo;


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
        // TODO: check permission
        return roleRepo.findAll(spec, pageable);
    }

    @Override
    public Role getRole(int id) {
        return roleRepo.findById(id)
                .orElseThrow(()-> new BizException(BaseResponseCode.ROLE_NOT_FOUND));
    }

    @Override
    public Role addRole(Role role) {
        // TODO: check permission
        if(roleRepo.existsById(role.getId())){
            throw new BizException(BaseResponseCode.ROLE_ALREADY_EXISTS);
        }
        return roleRepo.save(role);
    }

    @Override
    public Role updateRole(Role role) {
        // TODO: check permission
        if(role.getParent() != null && role.getParent().getId() != null){
            role.setParent(roleRepo.getReferenceById(role.getParent().getId()));
        }
        return roleRepo.save(role);
    }

    @Override
    public void deleteRole(int id) {
        // TODO: check permission
        roleRepo.deleteById(id);
    }

    @Override
    @Transactional
    public void assignPerms(int id, List<RolePermItemDTO> assignments) {
        Role role = this.getRole(id);
       rolePermRepo.saveAll(toRolePermissions(role, assignments, true));
    }

    @Override
    @Transactional
    public void replaceAllRolePerms(int id, List<RolePermItemDTO> replacements) {
        Role role = this.getRole(id);
        rolePermRepo.deleteByRoleId(id);
        if(CollectionUtil.isEmpty(replacements)) return;
        rolePermRepo.saveAll(toRolePermissions(role, replacements, false));
    }

    @Override
    @Transactional
    public void changeRolePerms(int id, List<RolePermItemDTO> updates, List<Integer> deletePermIds) {
        Role role = this.getRole(id);
        if(! CollectionUtil.isEmpty(deletePermIds)) {
            rolePermRepo.deleteByRoleIdAndPermissionIdIn(id,deletePermIds);
        };
        if(! CollectionUtil.isEmpty(updates)) {
            rolePermRepo.saveAll(toRolePermissions(role, updates, false));
        }
    }

    /**
     * convert RolePermItemDTO to RolePermission
     * @param role  role
     * @param items  items
     * @param strict whether check and throw the error of missing permissions.
     * @return role permissions
     */
    private List<RolePermission> toRolePermissions(Role role, List<RolePermItemDTO> items, boolean strict){
        Set<Integer> permIds = items.stream()
                .map(RolePermItemDTO::getPermissionId)
                .collect(Collectors.toSet());
        List<Permission> permEntities = permissionRepo.findAllById(permIds);

        Map<Integer, Permission> permMap = permEntities.stream()
                .collect(Collectors.toMap(Permission::getId, p -> p));
        if(strict && permIds.size() != permEntities.size()){
            List<Integer> notFounds = permIds.stream().filter(pId -> !permMap.containsKey(pId)).toList();
            throw new BizException(BaseResponseCode.PERMISSION_NOT_FOUND, notFounds);
        }
        return items.stream()
                .filter(dto -> permMap.containsKey(dto.getPermissionId()))
                .map(dto -> {
                    RolePermission rolePerm = new RolePermission();
                    rolePerm.setRole(role);
                    rolePerm.setPermission(permMap.get(dto.getPermissionId()));
                    rolePerm.setExpiresAt(dto.getExpiresAt() == null ? null : Instant.from(dto.getExpiresAt()));
                    return rolePerm;
                }).toList();
    }
}