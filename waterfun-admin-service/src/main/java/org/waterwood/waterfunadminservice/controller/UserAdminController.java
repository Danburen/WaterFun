package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.waterfunadminservice.dto.request.user.PatchUserRoleReq;
import org.waterwood.waterfunadminservice.dto.request.user.UpdateUserRoleReq;
import org.waterwood.waterfunadminservice.dto.request.user.AssignUserRoleReq;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.service.user.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/user")
public class UserAdminController {
    private final UserService userService;

    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoleToUser(@PathVariable long id, @Valid @RequestBody AssignUserRoleReq body){
        userService.assignRoles(id, body.getUserRoleItemDtos());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> updateRoleToUser(@PathVariable long id, @Valid @RequestBody UpdateUserRoleReq body){
        userService.replace(id, body.getUserRoleItemDtos());
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/roles")
    public ApiResponse<Void> patchRoleToUser(@PathVariable long id, @Valid @RequestBody PatchUserRoleReq body){
        userService.change(id, body.getAdds(), body.getDeletePermIds());
        return ApiResponse.success();
    }
}
