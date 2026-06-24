package org.waterwood.waterfunadminservice.service.ticket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.waterwood.waterfunservicecore.entity.BanPermission;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceImplTest {

    @Mock
    private PermissionRepo permissionRepo;

    @Mock
    private UserPermRepo userPermRepo;

    @Mock
    private UserRepository userRepository;

    private PenaltyServiceImpl penaltyService;

    private Permission banPostPermission;

    @BeforeEach
    void setUp() {
        banPostPermission = new Permission();
        banPostPermission.setId(1);
        banPostPermission.setCode(BanPermission.BAN_POST.getCode());
        banPostPermission.setName("Post Ban");

        penaltyService = new PenaltyServiceImpl(permissionRepo, userPermRepo, userRepository);
    }

    @Test
    void applyPenalty_shouldCreateUserPermission() {
        Long userUid = 100L;
        Mockito.when(permissionRepo.findByCode("ban:post")).thenReturn(Optional.of(banPostPermission));
        Mockito.when(userRepository.getReferenceById(userUid)).thenReturn(Mockito.mock(User.class));

        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        penaltyService.applyPenalty(userUid, PenaltyType.BAN_POST, BanReasonType.VIOLATION_OF_GUIDELINES, expiresAt);

        ArgumentCaptor<UserPermission> captor = ArgumentCaptor.forClass(UserPermission.class);
        Mockito.verify(userPermRepo).save(captor.capture());

        UserPermission saved = captor.getValue();
        Assertions.assertEquals(banPostPermission, saved.getPermission());
        Assertions.assertEquals(BanReasonType.VIOLATION_OF_GUIDELINES, saved.getBanReasonType());
        Assertions.assertNotNull(saved.getExpiresAt());
    }

    @Test
    void applyPenalty_withoutExpiry_shouldBePermanent() {
        Long userUid = 100L;
        Mockito.when(permissionRepo.findByCode("ban:post")).thenReturn(Optional.of(banPostPermission));
        Mockito.when(userRepository.getReferenceById(userUid)).thenReturn(Mockito.mock(User.class));

        penaltyService.applyPenalty(userUid, PenaltyType.BAN_POST, BanReasonType.INAPPROPRIATE_CONTENT, null);

        ArgumentCaptor<UserPermission> captor = ArgumentCaptor.forClass(UserPermission.class);
        Mockito.verify(userPermRepo).save(captor.capture());
        Assertions.assertNull(captor.getValue().getExpiresAt());
    }

    @Test
    void applyPenalty_unspecifiedOrOther_shouldDoNothing() {
        penaltyService.applyPenalty(100L, PenaltyType.UNSPECIFIED, null, null);
        penaltyService.applyPenalty(100L, PenaltyType.OTHER, null, null);

        Mockito.verifyNoInteractions(permissionRepo, userPermRepo, userRepository);
    }

    @Test
    void applyPenalty_permissionNotFound_shouldThrow() {
        Mockito.when(permissionRepo.findByCode(ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class,
                () -> penaltyService.applyPenalty(100L, PenaltyType.BAN_POST, null, null));
    }

    @Test
    void liftPenalty_shouldDeleteUserPermission() {
        Mockito.when(permissionRepo.findByCode("ban:post")).thenReturn(Optional.of(banPostPermission));

        penaltyService.liftPenalty(100L, PenaltyType.BAN_POST);

        Mockito.verify(userPermRepo).deleteByUserUidAndPermissionId(100L, banPostPermission.getId());
    }

    @Test
    void liftPenalty_unspecifiedOrOther_shouldDoNothing() {
        penaltyService.liftPenalty(100L, PenaltyType.UNSPECIFIED);
        penaltyService.liftPenalty(100L, PenaltyType.OTHER);

        Mockito.verifyNoInteractions(permissionRepo, userPermRepo);
    }

    @Test
    void applyPenalty_withNullReason_shouldUseDefault() {
        Long userUid = 100L;
        Mockito.when(permissionRepo.findByCode("ban:post")).thenReturn(Optional.of(banPostPermission));
        Mockito.when(userRepository.getReferenceById(userUid)).thenReturn(Mockito.mock(User.class));

        penaltyService.applyPenalty(userUid, PenaltyType.BAN_POST, null, null);

        ArgumentCaptor<UserPermission> captor = ArgumentCaptor.forClass(UserPermission.class);
        Mockito.verify(userPermRepo).save(captor.capture());
        Assertions.assertEquals(BanPermission.BAN_POST.getDefaultBanReason(), captor.getValue().getBanReasonType());
    }

    @Test
    void applyPenalty_forBanLogin_shouldWork() {
        Permission banLoginPermission = new Permission();
        banLoginPermission.setId(2);
        banLoginPermission.setCode(BanPermission.BAN_LOGIN.getCode());

        Mockito.when(permissionRepo.findByCode("ban:login")).thenReturn(Optional.of(banLoginPermission));
        Mockito.when(userRepository.getReferenceById(100L)).thenReturn(Mockito.mock(User.class));

        penaltyService.applyPenalty(100L, PenaltyType.BAN_LOGIN, BanReasonType.UNSPECIFIED, null);

        Mockito.verify(userPermRepo).save(ArgumentMatchers.any(UserPermission.class));
    }
}
