package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.TO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.role.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.PermissionResp;
import org.waterwood.waterfunadminservice.api.response.user.UserInfoARes;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PermissionMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserMapper;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunadminservice.api.response.role.RoleResp;
import org.waterwood.waterfunadminservice.infrastructure.mapper.RoleMapper;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.RoleSpec;
import org.waterwood.waterfunadminservice.service.role.RoleService;

import java.util.List;

/**
 * Role Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/role")
@PreAuthorize("isAuthenticated()")
public class RoleController {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final RoleService roleService;

    @GetMapping("/list")
    public ApiResponse<Page<RoleResp>> listRoles(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer parentId,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ){

        Specification<Role> spec = RoleSpec.of(name, code, parentId);
        Page<RoleResp> roles = roleService.listRoles(spec, pageable)
                .map(roleMapper::toRoleResp);
        return ApiResponse.success(roles);
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResp> getRole(@PathVariable int id){
        Role role = roleService.getRole(id);
        return ApiResponse.success(roleMapper.toRoleResp(role));
    }

    @PostMapping
    public ApiResponse<Void> addRole(@RequestBody CreateRoleRequest body){
        roleService.addRole(body);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateRole(@PathVariable int id, @RequestBody UpdateRoleRequest body){
        roleService.fullUpdateRole(id, body);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable int id){
        roleService.deleteRole(id);
        return ApiResponse.success();
    }
    
    @PostMapping("/{id}/permissions")
    public ApiResponse<BatchResult> assignPermissions(@PathVariable int id, @Valid @RequestBody AssignRolePermReq body){
        return ApiResponse.success(
                roleService.assignPerms(id, body.getPerms())
        );
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<BatchResult> updatePermissions(@PathVariable int id, @RequestBody UpdateRolePermReq body){
        return ApiResponse.success(
                roleService.replaceAllRolePerms(id, body.getPerms())
        );
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<List<PermissionResp>> listRolePerms(@PathVariable int id){
        List<Permission> perms = roleService.listRolePerms(id);
        return ApiResponse.success(perms.stream()
                .map(permissionMapper::toPermissionResp)
                .toList());
    }

    @DeleteMapping("/{id}/permissions")
    public ApiResponse<BatchResult> deleteRolePerms(@PathVariable int id, @RequestBody DeleteRolePermsReq req){
        return ApiResponse.success(roleService.removeRolePerms(id, req.getIds()));
    }

    @GetMapping("/{id}/users")
    public ApiResponse<Page<UserInfoARes>> getRoleUsers(@PathVariable int id,
                                                        @PageableDefault(page = 0, size = 10) Pageable pageable){
        Page<User> users = roleService.getRoleUsers(id, pageable);
        return ApiResponse.success(users.map(userMapper::toUserInfoARes));
    }

    @PostMapping("/{id}/users")
    public ApiResponse<BatchResult> assignUserRoles(@PathVariable int id, @RequestBody AssignUserToRoleReq req){
        return ApiResponse.success(roleService.assignUsers(id, req.getUserUids(), req.getExpiresAt()));
    }

    @PutMapping("/{id}/users")
    public ApiResponse<BatchResult> putUserRoles(@PathVariable int id, @RequestBody AssignUserToRoleReq req){
        return ApiResponse.success(roleService.replaceUserRoles(id, req.getUserUids(), req.getExpiresAt()));
    }

    @DeleteMapping("/{id}/users")
    public ApiResponse<BatchResult> deleteUserRoles(@PathVariable int id, @RequestBody RemoveRoleUsersReq req){
        return ApiResponse.success(
                roleService.removeRoleUsers(id, req.getUserIds())
        );
    }
}
