package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.waterfunadminservice.api.request.perm.CreatePermRequest;
import org.waterwood.waterfunadminservice.api.request.perm.UpdatePermRequest;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.PermissionResp;
import org.waterwood.waterfunadminservice.api.request.perm.PatchPermRequest;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PermissionMapper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.PermSpec;
import org.waterwood.waterfunadminservice.service.perm.PermissionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/permission")
public class PermissionController {
    private final PermissionService permissionService;
    private final PermissionMapper permissionMapper;

    @GetMapping("/list")
    public ApiResponse<Page<PermissionResp>> listUserPermissions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) PermissionType  type,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) Integer parentId,
            @PageableDefault(page = 0, size = 10) Pageable pageable){
        Specification<Permission> spec = PermSpec.of(name, type, resource, parentId);
        Page<PermissionResp> perms = permissionService.listPermissions(spec, pageable)
                .map(permissionMapper::toPermissionResp);
        return ApiResponse.success(perms);
    }

    @PostMapping
    public ApiResponse<Void> AddPermission(@RequestBody @Valid CreatePermRequest  body){
        permissionService.addPermission(permissionMapper.toEntity(body));
        return ApiResponse.success();
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> partialUpdatePermission(@PathVariable int id, @RequestBody @Valid PatchPermRequest body){
        Permission perm = permissionMapper
                .partialUpdate(body, permissionService.getPermission(id));
        permissionService.update(perm);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> UpdatePermission(@PathVariable int id, @RequestBody @Valid UpdatePermRequest body){
        Permission perm = permissionMapper
                .fullUpdate(body, permissionService.getPermission(id));
        permissionService.update(perm);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> DeletePermission(@PathVariable String id){
        permissionService.deleteUser(Integer.parseInt(id));
        return ApiResponse.success();
    }
}
