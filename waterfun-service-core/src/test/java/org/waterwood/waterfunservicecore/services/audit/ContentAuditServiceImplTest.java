package org.waterwood.waterfunservicecore.services.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.ReportPayload;
import org.waterwood.waterfunservicecore.entity.audit.*;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.ReportAlreadyExistException;
import org.waterwood.waterfunservicecore.exception.ReportNotFoundException;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.IdGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContentAuditServiceImpl")
class ContentAuditServiceImplTest {

    @Mock
    private AuditTaskResourceRepository auditTaskResourceRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private AuditTaskRepository auditTaskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContentAuditServiceImpl contentAuditService;

    private static final Long USER_UID = 100L;
    private static final Long TASK_ID = 500L;
    private static final String TARGET_ID = "200";
    private static final TargetType TARGET_TYPE = TargetType.POST;

    private User mockUser;
    private AuditPayload mockPayload;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        when(mockUser.getUid()).thenReturn(USER_UID);
        when(userRepository.getReferenceById(USER_UID)).thenReturn(mockUser);

        mockPayload = mock(AuditPayload.class);
        when(mockPayload.getFormat()).thenReturn(AuditContentFormat.RICH);
        when(mockPayload.toJson()).thenReturn("{\"content\":\"test\"}");
    }

    /* ==================== handleUserSubmit ==================== */

    @Nested
    @DisplayName("handleUserSubmit")
    class HandleUserSubmit {

        @Test
        @DisplayName("无已存在的 PENDING 任务时应创建新 AuditTask")
        void noExistingTask_createsNewTask() {
            when(auditTaskRepository.findByTargetIdAndTargetTypeAndStatus(
                    TARGET_ID, TARGET_TYPE, AuditStatus.PENDING
            )).thenReturn(Optional.empty());

            when(auditTaskRepository.save(any())).thenAnswer(invocation -> {
                AuditTask t = invocation.getArgument(0);
                if (t.getId() == null) t.setId(TASK_ID);
                return t;
            });

            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class);
                 MockedStatic<IdGenerator> idGen = mockStatic(IdGenerator.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.of(new AuthContext(USER_UID, "test-jti", "test-did", Locale.ENGLISH, null, null)));
                idGen.when(IdGenerator::generateAuditTaskId).thenReturn(TASK_ID);

                contentAuditService.handleUserSubmit(TARGET_ID, TARGET_TYPE, mockPayload, List.of());
            }

            ArgumentCaptor<AuditTask> captor = ArgumentCaptor.forClass(AuditTask.class);
            verify(auditTaskRepository).save(captor.capture());
            AuditTask saved = captor.getValue();

            assertEquals(TASK_ID, saved.getId());
            assertEquals(TARGET_ID, saved.getTargetId());
            assertEquals(TARGET_TYPE, saved.getTargetType());
            assertEquals(AuditStatus.PENDING, saved.getStatus());
            assertEquals(AuditTriggerType.USER_SUBMIT, saved.getTriggerType());
            assertEquals("{\"content\":\"test\"}", saved.getPayload());
            assertNotNull(saved.getSubmitAt());
            assertNotNull(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("已存在的 PENDING 任务应更新 payload 和 updatedAt")
        void existingPendingTask_updatesPayload() {
            AuditTask existingTask = new AuditTask();
            existingTask.setId(TASK_ID);
            existingTask.setTargetId(TARGET_ID);
            existingTask.setTargetType(TARGET_TYPE);
            existingTask.setStatus(AuditStatus.PENDING);
            existingTask.setSubmitAt(Instant.now().minusSeconds(3600));
            existingTask.setUpdatedAt(Instant.now().minusSeconds(3600));

            when(auditTaskRepository.findByTargetIdAndTargetTypeAndStatus(
                    TARGET_ID, TARGET_TYPE, AuditStatus.PENDING
            )).thenReturn(Optional.of(existingTask));

            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.of(new AuthContext(USER_UID, "test-jti", "test-did", Locale.ENGLISH, null, null)));

                contentAuditService.handleUserSubmit(TARGET_ID, TARGET_TYPE, mockPayload, List.of());
            }

            assertEquals(AuditStatus.PENDING, existingTask.getStatus());
            assertEquals("{\"content\":\"test\"}", existingTask.getPayload());
            assertNotNull(existingTask.getUpdatedAt());
            assertTrue(existingTask.getUpdatedAt().isAfter(Instant.now().minusSeconds(10)),
                    "updatedAt 应在本次操作中被更新");
        }

        @Test
        @DisplayName("未登录时应抛出异常")
        void unauthenticated_throws() {
            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.empty());

                assertThrows(ServiceException.class,
                        () -> contentAuditService.handleUserSubmit(TARGET_ID, TARGET_TYPE, mockPayload, List.of()));
            }
        }
    }

    /* ==================== handleUserReport ==================== */

    @Nested
    @DisplayName("handleUserReport")
    class HandleUserReport {

        @Test
        @DisplayName("无重复举报时应创建新举报任务")
        void noDuplicate_createsReport() {
            when(auditTaskRepository.findByTargetIdAndTargetTypeAndSubmitterUidAndStatus(
                    TARGET_ID, TARGET_TYPE, USER_UID, AuditStatus.PENDING
            )).thenReturn(Optional.empty());

            when(auditTaskRepository.save(any())).thenAnswer(invocation -> {
                AuditTask t = invocation.getArgument(0);
                if (t.getId() == null) t.setId(TASK_ID);
                return t;
            });

            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class);
                 MockedStatic<IdGenerator> idGen = mockStatic(IdGenerator.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.of(new AuthContext(USER_UID, "test-jti", "test-did", Locale.ENGLISH, null, null)));
                idGen.when(IdGenerator::generateAuditTaskId).thenReturn(TASK_ID);

                Long result = contentAuditService.handleUserReport(TARGET_ID, TARGET_TYPE, AuditType.VIOLATION_OF_GUIDELINES, "违规内容");

                assertEquals(TASK_ID, result);
            }

            ArgumentCaptor<AuditTask> captor = ArgumentCaptor.forClass(AuditTask.class);
            verify(auditTaskRepository).save(captor.capture());
            AuditTask saved = captor.getValue();

            assertEquals(AuditTriggerType.USER_REPORT, saved.getTriggerType());
            assertEquals(AuditStatus.PENDING, saved.getStatus());
            assertTrue(saved.getPayload().contains("违规内容"));
            assertTrue(saved.getPayload().contains(USER_UID.toString()));
        }

        @Test
        @DisplayName("重复 PENDING 举报时应抛出 ReportAlreadyExistException")
        void duplicatePendingReport_throws() {
            AuditTask existing = new AuditTask();
            existing.setId(1L);
            when(auditTaskRepository.findByTargetIdAndTargetTypeAndSubmitterUidAndStatus(
                    TARGET_ID, TARGET_TYPE, USER_UID, AuditStatus.PENDING
            )).thenReturn(Optional.of(existing));

            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.of(new AuthContext(USER_UID, "test-jti", "test-did", Locale.ENGLISH, null, null)));

                assertThrows(ReportAlreadyExistException.class,
                        () -> contentAuditService.handleUserReport(TARGET_ID, TARGET_TYPE, AuditType.VIOLATION_OF_GUIDELINES, "违规内容"));
            }

            verify(auditTaskRepository, never()).save(any());
        }

        @Test
        @DisplayName("未登录时应抛出异常")
        void unauthenticated_throws() {
            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.empty());

                assertThrows(ServiceException.class,
                        () -> contentAuditService.handleUserReport(TARGET_ID, TARGET_TYPE, AuditType.VIOLATION_OF_GUIDELINES, "理由"));
            }
        }
    }

    /* ==================== cancelUserReport ==================== */

    @Nested
    @DisplayName("cancelUserReport")
    class CancelUserReport {

        @Test
        @DisplayName("已存在的 PENDING 举报应取消")
        void existingPendingReport_cancels() {
            AuditTask task = new AuditTask();
            task.setId(TASK_ID);
            task.setStatus(AuditStatus.PENDING);

            when(auditTaskRepository.findByTargetIdAndTargetTypeAndSubmitterUidAndStatus(
                    TARGET_ID, TARGET_TYPE, USER_UID, AuditStatus.PENDING
            )).thenReturn(Optional.of(task));

            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.of(new AuthContext(USER_UID, "test-jti", "test-did", Locale.ENGLISH, null, null)));

                contentAuditService.cancelUserReport(TARGET_ID, TARGET_TYPE);
            }

            assertEquals(AuditStatus.CANCELED, task.getStatus());
            assertNotNull(task.getUpdatedAt());
            verify(auditTaskRepository).save(task);
        }

        @Test
        @DisplayName("不存在的举报应抛出 ReportNotFoundException")
        void noReport_throws() {
            when(auditTaskRepository.findByTargetIdAndTargetTypeAndSubmitterUidAndStatus(
                    TARGET_ID, TARGET_TYPE, USER_UID, AuditStatus.PENDING
            )).thenReturn(Optional.empty());

            try (MockedStatic<UserCtxHolder> ctx = mockStatic(UserCtxHolder.class)) {
                ctx.when(UserCtxHolder::safeGet).thenReturn(Optional.of(new AuthContext(USER_UID, "test-jti", "test-did", Locale.ENGLISH, null, null)));

                assertThrows(ReportNotFoundException.class,
                        () -> contentAuditService.cancelUserReport(TARGET_ID, TARGET_TYPE));
            }
        }
    }
}
