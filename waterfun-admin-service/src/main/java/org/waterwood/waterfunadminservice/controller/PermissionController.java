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
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.waterfunadminservice.api.request.perm.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.perm.PermissionResp;
import org.waterwood.waterfunadminservice.api.response.user.AssignedUserRes;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PermissionMapper;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RequireRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.PermSpec;
import org.waterwood.waterfunadminservice.service.perm.PermissionService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/permission")
@RequireRole("ADMIN")
public class PermissionController {
    private final PermissionService permissionService;
    private final PermissionMapper permissionMapper;

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

    @GetMapping("/{id}")
    public ApiResponse<PermissionResp> getPermission(@PathVariable int id) {
        Permission permission = permissionService.getPermission(id);
        return ApiResponse.success(permissionMapper.toPermissionResp(permission));
    }

    @PostMapping
    public ApiResponse<Void> addPermission(@RequestBody @Valid CreatePermRequest  body){
        permissionService.addPermission(body);
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<BatchResult> deletePerms(@RequestBody DeletePermsRequest req) {
        return ApiResponse.success(
                permissionService.removePerms(req)
        );
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
    public ApiResponse<Page<AssignedUserRes>> listPermUsers(@PathVariable int id,
                                                            @RequestParam(required = false) Long userUid,
                                                            @RequestParam(required = false) String username,
                                                            @RequestParam(required = false) String nickname,
                                                            @PageableDefault Pageable pageable){
        return ApiResponse.success(permissionService.listPermUsers(id, userUid, username, nickname, pageable));
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

    @GetMapping("/options")
    public ApiResponse<List<OptionVO<Integer>>> getPermOptions(){
        return ApiResponse.success(
                permissionService.getAllPermOptions()
        );
    }
}
