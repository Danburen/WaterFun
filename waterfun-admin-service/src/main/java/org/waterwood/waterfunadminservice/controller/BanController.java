package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.request.security.BanUserRequest;
import org.waterwood.waterfunadminservice.api.request.security.LiftPenaltyRequest;
import org.waterwood.waterfunadminservice.api.response.BanStatusResponse;
import org.waterwood.waterfunadminservice.api.response.BanUserResponse;
import org.waterwood.waterfunadminservice.service.ticket.PenaltyService;
import org.waterwood.waterfunservicecore.entity.BanPermission;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/bans")
@RequiredArgsConstructor
public class BanController {

    private final PenaltyService penaltyService;
    private final UserPermRepo userPermRepo;
    private final PermissionRepo permissionRepo;

    @GetMapping
    public ApiResponse<Page<BanUserResponse>> listBans(
            @RequestParam(required = false) Long userUid,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Set<Integer> banPermIds = getBanPermissionIds();
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<UserPermission> permPage = userPermRepo.listPermUsersByPermIds(
                banPermIds, userUid, username, nickname, pageable);
        return ApiResponse.success(permPage.map(this::toBanResponse));
    }

    @PostMapping
    public ApiResponse<Void> banUser(@Valid @RequestBody BanUserRequest req) {
        Instant expiresAt = req.getPenaltyDurationHours() != null
                ? Instant.now().plusSeconds(req.getPenaltyDurationHours() * 3600)
                : null;
        penaltyService.applyPenalty(req.getUserUid(), req.getPenaltyType(),
                req.getBanReasonType(), expiresAt,
                null, null, req.getReasonText());
        return ApiResponse.success();
    }

    @PostMapping("/{userUid}/lift")
    public ApiResponse<Void> liftPenalty(@PathVariable Long userUid,
                                          @Valid @RequestBody LiftPenaltyRequest req) {
        if (req.getPenaltyType() != null) {
            penaltyService.liftPenalty(userUid, req.getPenaltyType());
        } else {
            penaltyService.liftAllPenalties(userUid);
        }
        return ApiResponse.success();
    }

    @PostMapping("/{userUid}/lift-all")
    public ApiResponse<Void> liftAllPenalties(@PathVariable Long userUid) {
        penaltyService.liftAllPenalties(userUid);
        return ApiResponse.success();
    }

    @GetMapping("/status")
    public ApiResponse<BanStatusResponse> getBanStatus(@RequestParam Long userUid) {
        Set<Integer> banPermIds = getBanPermissionIds();
        List<UserPermission> activePerms = userPermRepo.findByUserUidAndPermissionIdIn(userUid, banPermIds);

        Instant now = Instant.now();
        List<UserPermission> activeBans = activePerms.stream()
                .filter(up -> up.getExpiresAt() == null || up.getExpiresAt().isAfter(now))
                .toList();

        List<BanStatusResponse.ActiveRestriction> restrictions = activeBans.stream()
                .map(up -> BanStatusResponse.ActiveRestriction.builder()
                        .permissionCode(up.getCode())
                        .permissionName(up.getName())
                        .banReasonType(up.getBanReasonType() != null ? up.getBanReasonType().name() : null)
                        .expiresAt(up.getExpiresAt())
                        .permanent(up.getExpiresAt() == null)
                        .createdAt(up.getCreatedAt())
                        .build())
                .toList();

        BanStatusResponse resp = BanStatusResponse.builder()
                .userUid(userUid)
                .banned(!restrictions.isEmpty())
                .restrictions(restrictions)
                .build();
        return ApiResponse.success(resp);
    }

    private Set<Integer> getBanPermissionIds() {
        Set<String> banCodes = Arrays.stream(BanPermission.values())
                .map(BanPermission::getCode)
                .collect(Collectors.toSet());
        return permissionRepo.findByCodeIn(List.copyOf(banCodes)).stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());
    }

    private BanUserResponse toBanResponse(UserPermission up) {
        return BanUserResponse.builder()
                .userUid(up.getUser().getUid())
                .displayName(up.getUser().getDisplayName())
                .nickname(up.getUser().getNickname())
                .permissionName(up.getName())
                .permissionCode(up.getCode())
                .expiresAt(up.getExpiresAt())
                .createdAt(up.getCreatedAt())
                .build();
    }
}
