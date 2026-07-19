package org.waterwood.waterfunadminservice.service.ticket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.entity.BanPermission;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPenaltyHistory;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPenaltyHistoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final PermissionRepo permissionRepo;
    private final UserPermRepo userPermRepo;
    private final UserRepository userRepository;
    private final UserPenaltyHistoryRepository userPenaltyHistoryRepository;

    @Transactional
    @Override
    public void applyPenalty(Long userUid, PenaltyType penaltyType, BanReasonType reason, Instant expiresAt,
                             String targetId, TargetType targetType, String reasonText) {
        if (penaltyType == PenaltyType.UNSPECIFIED || penaltyType == PenaltyType.OTHER) {
            return;
        }

        String permissionCode = penaltyType.getBanPermission().getCode();
        Permission permission = permissionRepo.findByCode(permissionCode)
                .orElseThrow(() -> new NotFoundException("Permission not found for code: " + permissionCode));

        BanReasonType banReason = reason != null ? reason : penaltyType.getBanPermission().getDefaultBanReason();
        User user = userRepository.getReferenceById(userUid);

        // Extend existing restriction if already present, otherwise create a new one
        userPermRepo.findByUserUidAndPermissionId(userUid, permission.getId())
                .ifPresentOrElse(
                        existing -> {
                            if (expiresAt == null) {
                                // New penalty is permanent → override existing
                                existing.setExpiresAt(null);
                                existing.setBanReasonType(banReason);
                                userPermRepo.save(existing);
                            } else if (existing.getExpiresAt() == null) {
                                log.warn("User {} already has a permanent {} ban; temporary extension ignored",
                                        userUid, permissionCode);
                            } else if (expiresAt.isAfter(existing.getExpiresAt())) {
                                // New expiry is later → extend
                                existing.setExpiresAt(expiresAt);
                                existing.setBanReasonType(banReason);
                                userPermRepo.save(existing);
                            }
                            // Otherwise existing is already longer or equal → no change
                        },
                        () -> {
                            UserPermission userPermission = new UserPermission();
                            userPermission.setUser(user);
                            userPermission.setPermission(permission);
                            userPermission.setExpiresAt(expiresAt);
                            userPermission.setBanReasonType(banReason);
                            userPermRepo.save(userPermission);
                        }
                );

        UserPenaltyHistory history = new UserPenaltyHistory();
        history.setUser(user);
        history.setPenaltyType(penaltyType);
        history.setTargetId(targetId);
        history.setTargetType(targetType);
        history.setPenaltyReasonType(banReason != null ? banReason.toAuditType() : AuditType.OTHER);
        history.setReason(reasonText);
        history.setOperator(UserCtxHolder.safeGetUserId().map(userRepository::getReferenceById).orElse(null));
        history.setCreatedAt(Instant.now());
        userPenaltyHistoryRepository.save(history);
    }

    @Transactional
    @Override
    public void liftPenalty(Long userUid, PenaltyType penaltyType) {
        if (penaltyType == PenaltyType.UNSPECIFIED || penaltyType == PenaltyType.OTHER) {
            return;
        }

        String permissionCode = penaltyType.getBanPermission().getCode();
        Permission permission = permissionRepo.findByCode(permissionCode)
                .orElseThrow(() -> new NotFoundException("Permission not found for code: " + permissionCode));

        userPermRepo.deleteByUserUidAndPermissionId(userUid, permission.getId());

        UserPenaltyHistory history = new UserPenaltyHistory();
        history.setUser(userRepository.getReferenceById(userUid));
        history.setPenaltyType(penaltyType);
        history.setPenaltyReasonType(AuditType.OTHER);
        history.setReason("Penalty lifted - " + permissionCode);
        history.setOperator(UserCtxHolder.safeGetUserId().map(userRepository::getReferenceById).orElse(null));
        history.setCreatedAt(Instant.now());
        userPenaltyHistoryRepository.save(history);
    }

    @Transactional
    @Override
    public void liftAllPenalties(Long userUid) {
        List<String> banCodes = Arrays.stream(BanPermission.values())
                .map(BanPermission::getCode)
                .toList();
        List<Permission> banPermissions = permissionRepo.findByCodeIn(banCodes);
        if (banPermissions.isEmpty()) return;
        Set<Integer> banPermissionIds = banPermissions.stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        Set<UserPermission> currentPerms = userPermRepo.findByUserUid(userUid);
        if (currentPerms.isEmpty()) return;

        userPermRepo.deleteByUserUidAndPermissionIdIn(userUid, banPermissionIds);

        UserPenaltyHistory history = new UserPenaltyHistory();
        history.setUser(userRepository.getReferenceById(userUid));
        history.setPenaltyType(PenaltyType.OTHER);
        history.setPenaltyReasonType(AuditType.OTHER);
        history.setReason("Appeal approved - all restrictions lifted");
        history.setOperator(UserCtxHolder.safeGetUserId().map(userRepository::getReferenceById).orElse(null));
        history.setCreatedAt(Instant.now());
        userPenaltyHistoryRepository.save(history);
    }
}
