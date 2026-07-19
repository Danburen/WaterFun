package org.waterwood.waterfunservice.service.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.post.Comment;
import org.waterwood.waterfunservicecore.entity.post.CommentStatus;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CommentRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentModerationCallbackStrategy implements ModerationCallbackStrategy {

    private final AuditTaskRepository auditTaskRepository;
    private final CommentRepository commentRepository;
    private final ModerationInboxHandler moderationInboxHandler;

    @Override
    public Set<TargetType> getTargetTypes() {
        return Set.of(TargetType.COMMENT);
    }

    @Transactional
    @Override
    public void handle(ModerationConsumerMessage msg) {
        AuditTask task = auditTaskRepository.findById(msg.getId())
                .orElseThrow(() -> new IllegalStateException("AuditTask not found: " + msg.getId()));
        Long commentId = Long.valueOf(task.getTargetId());
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalStateException("Comment not found: " + commentId));

        if (msg.getStatus() == AuditStatus.APPROVED) {
            log.info("Comment {} approved by moderation", commentId);
        } else {
            comment.setStatus(CommentStatus.REJECTED);
            commentRepository.save(comment);
            log.info("Comment {} rejected by moderation", commentId);
        }

        moderationInboxHandler.handleModeration(msg, comment.getAuthor().getUid());
    }

    @Transactional
    @Override
    public void handleBatch(List<ModerationConsumerMessage> msgs) {
        if (msgs.isEmpty()) return;

        List<Long> allTaskIds = msgs.stream()
                .map(ModerationConsumerMessage::getId)
                .distinct()
                .toList();
        Map<Long, AuditTask> taskMap = auditTaskRepository.findAllById(allTaskIds)
                .stream()
                .collect(Collectors.toMap(AuditTask::getId, Function.identity()));

        List<Long> allCommentIds = taskMap.values().stream()
                .map(t -> Long.valueOf(t.getTargetId()))
                .distinct()
                .toList();
        Map<Long, Comment> commentMap = commentRepository.findAllById(allCommentIds)
                .stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));

        Map<AuditStatus, List<ModerationConsumerMessage>> byStatus = msgs.stream()
                .collect(Collectors.groupingBy(ModerationConsumerMessage::getStatus));

        byStatus.forEach((status, messages) -> {
            if (status == AuditStatus.REJECTED) {
                List<Comment> toDelete = messages.stream()
                        .map(m -> taskMap.get(m.getId()))
                        .filter(java.util.Objects::nonNull)
                        .map(t -> commentMap.get(Long.valueOf(t.getTargetId())))
                        .filter(java.util.Objects::nonNull)
                        .toList();
                toDelete.forEach(c -> c.setStatus(CommentStatus.REJECTED));
                commentRepository.saveAll(toDelete);
                log.info("Batch rejected {} comments", toDelete.size());
            } else {
                log.info("Batch approved {} comments", messages.size());
            }
        });

        moderationInboxHandler.handleBatch(msgs);
    }
}
