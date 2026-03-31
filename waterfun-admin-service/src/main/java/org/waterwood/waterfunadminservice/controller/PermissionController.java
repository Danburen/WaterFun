package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.TO.BatchResult;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.waterfunadminservice.api.request.perm.CreatePermRequest;
import org.waterwood.waterfunadminservice.api.request.perm.UpdatePermRequest;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.request.perm.assignPermToUsersReq;
import org.waterwood.waterfunadminservice.api.request.perm.removePermUsersReq;
import org.waterwood.waterfunadminservice.api.response.PermissionResp;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminDetail;
import org.waterwood.waterfunadminservice.api.response.user.UserInfoARes;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserAdminMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserMapper;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PermissionMapper;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.PermSpec;
import org.waterwood.waterfunadminservice.service.perm.PermissionService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/permission")
@PreAuthorize("isAuthenticated()")
public class PermissionController {
    private final PermissionService permissionService;
    private final PermissionMapper permissionMapper;
    private final UserAdminMapper userAdminMapper;
    private final UserMapper userMapper;

    @GetMapping("/list")
    public ApiResponse<Page<PermissionResp>> listPermissions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) PermissionType  type,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) Integer parentId,
            @PageableDefault(page = 0, size = 10) Pageable pageable){
        Specification<Permission> spec = PermSpec.of(name, code, type, resource, parentId);
        Page<PermissionResp> perms = permissionService.listPermissions(spec, pageable)
                .map(permissionMapper::toPermissionResp);
        return ApiResponse.success(perms);
    }

    @PostMapping
    public ApiResponse<Void> addPermission(@RequestBody @Valid CreatePermRequest  body){
        permissionService.addPermission(body);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updatePermission(@PathVariable int id, @RequestBody @Valid UpdatePermRequest body){
        permissionService.fullUpdate(id, body);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePermission(@PathVariable String id){
        permissionService.deletePerm(Integer.parseInt(id));
        return ApiResponse.success();
    }

    @GetMapping("/{id}/users")
    public ApiResponse<Page<UserInfoARes>> getPermUsers(@PathVariable int id,
                                                        @PageableDefault(page = 0, size = 10) Pageable pageable){
        Page<User> users = permissionService.listPermUsers(id, pageable);
        return ApiResponse.success(users.map(userMapper::toUserInfoARes));
    }

    @PostMapping("/{id}/users")
    public ApiResponse<BatchResult> assignPermToUsers(@PathVariable int id, @RequestBody @Valid assignPermToUsersReq body){
        return ApiResponse.success(
                permissionService.assignPermToUsers(id, body.getIds(), body.getExpiresAt())
        );
    }

    @PutMapping("/{id}/users")
    public ApiResponse<BatchResult> putPermUsers(@PathVariable int id, @RequestBody @Valid assignPermToUsersReq body){
        return ApiResponse.success(
                permissionService.replacePermUsers(id, body.getIds(), body.getExpiresAt())
        );
    }

    @DeleteMapping("/{id}/users")
    public ApiResponse<BatchResult> deletePermUsers(@PathVariable int id, @RequestBody @Valid removePermUsersReq body){
        return ApiResponse.success(
                permissionService.removePermUsers(id, body.getUserUids())
        );
    }
}
