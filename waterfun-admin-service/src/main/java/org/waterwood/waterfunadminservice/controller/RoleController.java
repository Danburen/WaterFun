package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.waterwood.waterfunadminservice.api.request.role.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunadminservice.api.response.role.RoleResp;
import org.waterwood.waterfunadminservice.infrastructure.mapper.RoleMapper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.RoleSpec;
import org.waterwood.waterfunadminservice.service.role.RoleService;

/**
 * Role Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/role")
public class RoleController {
    private final RoleService roleService;
    private final RoleMapper roleMapper;

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<RoleResp>> listRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer parentId
    ){

        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Specification<Role> spec = RoleSpec.of(name, parentId);
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
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> addRole(@RequestBody CreateRoleRequest body){
        roleService.addRole(roleMapper.toEntity(body));
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateRole(@PathVariable int id, @RequestBody UpdateRoleRequest body){
        Role role = roleMapper.fullUpdate(body, roleService.getRole(id));
        roleService.updateRole(role);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> partialUpdateRole(@PathVariable int id, @RequestBody PatchRoleRequest body){
        Role role = roleMapper.partialUpdate(body, roleService.getRole(id));
        roleService.updateRole(role);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable int id){
        roleService.deleteRole(id);
        return ApiResponse.success();
    }
    
    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable int id,@Valid @RequestBody AssignRolePermReq body){
        roleService.assignPerms(id, body.getPermsDto());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> updatePermissions(@PathVariable int id, @RequestBody UpdateRolePermReq body){
        roleService.replaceAllRolePerms(id, body.getPermsDto());
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/permissions")
    public ApiResponse<Void> partialUpdatePermissions(@PathVariable int id, @Valid @RequestBody PatchRolePermReq body){
        roleService.changeRolePerms(id, body.getUpdates(), body.getDeletePermIds());
        return ApiResponse.success();
    }
}
