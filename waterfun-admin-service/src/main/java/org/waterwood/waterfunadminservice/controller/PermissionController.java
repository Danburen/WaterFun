package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.perm.*;
import org.waterwood.waterfunadminservice.api.response.perm.PermissionResp;
import org.waterwood.waterfunadminservice.api.response.user.AssignedUserRes;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PermissionMapper;
import org.waterwood.waterfunadminservice.service.perm.PermissionService;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.perm.PermissionType;
import org.waterwood.waterfunservicecore.entity.spec.PermSpec;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/permission")
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
        // frontend sends 1-based page, Spring Data Pageable is 0-based
        pageable = PageRequest.of(Math.max(0, pageable.getPageNumber() - 1), pageable.getPageSize(), pageable.getSort());
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
    public ApiResponse<Void> deletePermission(@PathVariable Integer id){
        permissionService.deletePerm(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/users")
    public ApiResponse<Page<AssignedUserRes>> listPermUsers(@PathVariable int id,
                                                            @RequestParam(required = false) Long userUid,
                                                            @RequestParam(required = false) String username,
                                                            @RequestParam(required = false) String nickname,
                                                            @PageableDefault Pageable pageable){
        pageable = PageRequest.of(Math.max(0, pageable.getPageNumber() - 1), pageable.getPageSize(), pageable.getSort());
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
