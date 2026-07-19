package org.waterwood.waterfunservice.service.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.post.Comment;
import org.waterwood.waterfunservicecore.entity.post.CommentStatus;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CommentRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommentModerationCallbackStrategy")
class CommentModerationCallbackStrategyTest {

    @Mock
    private AuditTaskRepository auditTaskRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ModerationInboxHandler moderationInboxHandler;

    @InjectMocks
    private CommentModerationCallbackStrategy strategy;

    private static final Long TASK_ID = 500L;
    private static final Long COMMENT_ID = 300L;
    private static final Long AUTHOR_UID = 100L;

    private AuditTask mockTask;
    private Comment mockComment;
    private User mockAuthor;

    @BeforeEach
    void setUp() {
        mockAuthor = mock(User.class);
        when(mockAuthor.getUid()).thenReturn(AUTHOR_UID);

        mockTask = new AuditTask();
        mockTask.setId(TASK_ID);
        mockTask.setTargetId(COMMENT_ID.toString());
        mockTask.setTargetType(TargetType.COMMENT);

        mockComment = mock(Comment.class);
        when(mockComment.getAuthor()).thenReturn(mockAuthor);
        when(mockComment.getStatus()).thenReturn(CommentStatus.NORMAL);
    }

    private ModerationConsumerMessage createMessage(AuditStatus status) {
        return ModerationConsumerMessage.builder()
                .id(TASK_ID)
                .targetId(COMMENT_ID.toString())
                .targetType(TargetType.COMMENT)
                .status(status)
                .sendTime(Instant.now())
                .build();
    }

    /* ==================== Single Handle ==================== */

    @Nested
    @DisplayName("handle (单条)")
    class SingleHandle {

        @Test
        @DisplayName("审核通过时评论状态不变")
        void approved_keepsCommentNormal() {
            when(auditTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(mockTask));
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(mockComment));

            strategy.handle(createMessage(AuditStatus.APPROVED));

            verify(commentRepository, never()).save(any());
            verify(moderationInboxHandler).handleModeration(any(), eq(AUTHOR_UID));
        }

        @Test
        @DisplayName("审核驳回时将评论状态设为 REJECTED（非 DELETED）")
        void rejected_setsCommentRejected() {
            when(auditTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(mockTask));
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(mockComment));

            strategy.handle(createMessage(AuditStatus.REJECTED));

            verify(mockComment).setStatus(CommentStatus.REJECTED);
            verify(commentRepository).save(mockComment);
            verify(moderationInboxHandler).handleModeration(any(), eq(AUTHOR_UID));
        }

        @Test
        @DisplayName("任务不存在时应抛出异常")
        void taskNotFound_throws() {
            when(auditTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> strategy.handle(createMessage(AuditStatus.APPROVED)));
        }

        @Test
        @DisplayName("评论不存在时应抛出异常")
        void commentNotFound_throws() {
            when(auditTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(mockTask));
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> strategy.handle(createMessage(AuditStatus.APPROVED)));
        }
    }

    /* ==================== Batch Handle ==================== */

    @Nested
    @DisplayName("handleBatch (批量)")
    class BatchHandle {

        @Test
        @DisplayName("批量驳回时所有评论设为 REJECTED")
        void batchRejected_setsAllRejected() {
            Long commentId2 = 301L;
            Long taskId2 = 501L;

            Comment mockComment2 = mock(Comment.class);
            when(mockComment2.getAuthor()).thenReturn(mockAuthor);

            AuditTask task2 = new AuditTask();
            task2.setId(taskId2);
            task2.setTargetId(commentId2.toString());
            task2.setTargetType(TargetType.COMMENT);

            ModerationConsumerMessage msg1 = createMessage(AuditStatus.REJECTED);
            ModerationConsumerMessage msg2 = ModerationConsumerMessage.builder()
                    .id(taskId2)
                    .targetId(commentId2.toString())
                    .targetType(TargetType.COMMENT)
                    .status(AuditStatus.REJECTED)
                    .sendTime(Instant.now())
                    .build();

            when(auditTaskRepository.findAllById(List.of(TASK_ID, taskId2)))
                    .thenReturn(List.of(mockTask, task2));
            when(commentRepository.findAllById(List.of(COMMENT_ID, commentId2)))
                    .thenReturn(List.of(mockComment, mockComment2));

            strategy.handleBatch(List.of(msg1, msg2));

            verify(mockComment).setStatus(CommentStatus.REJECTED);
            verify(mockComment2).setStatus(CommentStatus.REJECTED);
            verify(commentRepository).saveAll(anyList());
            verify(moderationInboxHandler).handleBatch(anyList());
        }

        @Test
        @DisplayName("批量通过时评论状态不变")
        void batchApproved_changesNothing() {
            ModerationConsumerMessage msg = createMessage(AuditStatus.APPROVED);

            strategy.handleBatch(List.of(msg));

            verify(commentRepository, never()).saveAll(anyList());
            verify(commentRepository, never()).save(any());
            verify(moderationInboxHandler).handleBatch(anyList());
        }
    }
}
