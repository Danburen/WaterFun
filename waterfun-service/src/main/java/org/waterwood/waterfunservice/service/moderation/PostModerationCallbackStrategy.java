package org.waterwood.waterfunservice.service.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.service.post.TagService;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class PostModerationCallbackStrategy implements ModerationCallbackStrategy {
    private final AuditTaskRepository auditTaskRepository;
    private final ModerationConsumeHandler moderationConsumeHandler;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final TagService tagService;
    private final TagRepository tagRepository;

    @Override
    public Set<TargetType> getTargetTypes() {
        return Set.of(
                TargetType.POST,
                TargetType.POST_CONTENT_IMAGE,
                TargetType.POST_COVERAGE_IMAGE
        );
    }

    @Override
    public void handle(ModerationConsumerMessage msg) {
        AuditTask task = auditTaskRepository.findById(msg.getId())
                .orElseThrow(() -> new IllegalStateException("AuditTask not found for id: " + msg.getId()));
        Post p = postRepository.findByIdAndIsDeletedAndStatus(
                Long.valueOf(task.getTargetId()),
                false,
                PostStatus.PENDING
        ).orElseThrow(() -> new IllegalStateException("Post not found for id: " + task.getTargetId()));
        User u = p.getAuthor();
        moderationConsumeHandler.handleModeration(msg, u.getUid());
        switch (msg.getTargetType()) {
            case POST -> handlePostAuditCallback(msg, p, u.getUid());
            default -> throw new IllegalStateException("Unexpected target type: " + msg.getTargetType());
        }
    }

    private void handlePostAuditCallback(ModerationConsumerMessage msg, Post p, Long userUid) {
        if(msg.getStatus() == AuditStatus.APPROVED){
            List<Tag> newTagList = new ArrayList<>();
            if(CollectionUtil.isEmpty(p.getEditedNewTags())){
                newTagList = tagService.createNewTags(
                        p.getEditedNewTags().stream()
                                .filter(StringUtil::isNotBlank)
                                .collect(Collectors.toSet()),
                        userUid
                );
            }
            p.setTitle(p.getEditedTitle());
            p.setSubtitle(p.getEditedSubtitle());
            p.setContent(p.getEditedContent());
            p.setSummary(p.getEditedSummary());
            p.setCategory(p.getEditedCategory());
            List<Tag> existsTags = tagRepository.findAllById(p.getEditedTagIds());
            List<Tag> allTags = Stream.concat(
                            newTagList.stream(),
                            existsTags.stream()
                    )
                    .collect(Collectors.toMap(
                            Tag::getId,
                            tag -> tag,
                            (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .toList();
            p.setTags(allTags);
            p.setVersion(p.getVersion() + 1);
            p.setStatus(PostStatus.PUBLISHED);
            postRepository.save(p);
        } else {
            p.setStatus(PostStatus.REJECTED);
            postRepository.save(p);
        }
    }
}
