package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.role.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.perm.AssignedPermissionRes;
import org.waterwood.waterfunadminservice.api.response.user.AssignedUserRes;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunadminservice.api.response.role.RoleResp;
import org.waterwood.waterfunadminservice.infrastructure.mapper.RoleMapper;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RequireRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.RoleSpec;
import org.waterwood.waterfunadminservice.service.role.RoleService;

import java.util.List;

/**
 * Role Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/role")
@RequireRole("ADMIN")
public class RoleController {
    private final RoleMapper roleMapper;
    private final RoleService roleService;

    @GetMapping("/list")
    public ApiResponse<Page<RoleResp>> listRoles(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer parentId,
            @PageableDefault() Pageable pageable
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

    @DeleteMapping
    public ApiResponse<BatchResult> deleteRoles(@RequestBody DeleteRolesRequest req){
        return ApiResponse.success(
                roleService.removeRoles(req)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateRole(@PathVariable int id, @RequestBody UpdateRoleRequest body){
        roleService.updateRole(id, body);
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
    public ApiResponse<BatchResult> putPermissions(@PathVariable int id, @RequestBody UpdateRolePermReq body){
        return ApiResponse.success(
                roleService.replaceAllRolePerms(id, body.getPerms())
        );
    }

    @DeleteMapping("/{id}/permissions")
    public ApiResponse<BatchResult> deleteRolePerms(@PathVariable int id, @RequestBody DeleteRolePermsReq req){
        return ApiResponse.success(roleService.removeRolePerms(id, req.getIds()));
    }

    @GetMapping("/{id}/users")
    public ApiResponse<Page<AssignedUserRes>> listRoleUsers(@PathVariable int id,
                                                            @RequestParam(required = false) Long userUid,
                                                            @RequestParam(required = false) String username,
                                                            @RequestParam(required = false) String nickname,
                                                            @PageableDefault() Pageable pageable){
        return ApiResponse.success(roleService.getRoleUsers(id, userUid, username, nickname, pageable));
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<Page<AssignedPermissionRes>> getRolePerms(@PathVariable int id,
                                                                 @RequestParam(required = false) Integer permId,
                                                                 @RequestParam(required = false) String code,
                                                                 @RequestParam(required = false) String name,
                                                                 @PageableDefault Pageable pageable){
        return ApiResponse.success(roleService.getRolePerms(id, permId, code, name, pageable));
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

    @GetMapping("/options")
    public ApiResponse<List<OptionVO<Integer>>> listRoleOptions(){
        return ApiResponse.success(
                roleService.getAllRoleOptions()
        );
    }
}
