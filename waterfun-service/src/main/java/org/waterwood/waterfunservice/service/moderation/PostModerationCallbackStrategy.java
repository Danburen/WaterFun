package org.waterwood.waterfunservice.service.moderation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.waterwood.waterfunservicecore.exception.TagLimitExceededException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostModerationCallbackStrategy implements ModerationCallbackStrategy {
    private final AuditTaskRepository auditTaskRepository;
    private final ModerationInboxHandler moderationInboxHandler;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final TagService tagService;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Set<TargetType> getTargetTypes() {
        return Set.of(
                TargetType.POST,
                TargetType.POST_CONTENT_IMAGE,
                TargetType.POST_COVERAGE_IMAGE
        );
    }

    @Transactional
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
        switch (msg.getTargetType()) {
            case POST -> handlePostAuditCallback(msg, p, u.getUid());
            default -> log.warn("Unexpected target type: " + msg.getTargetType());
        }
        moderationInboxHandler.handleModeration(msg, u.getUid());
    }

    private void handlePostAuditCallback(ModerationConsumerMessage msg, Post p, Long userUid) {
        if(msg.getStatus() == AuditStatus.APPROVED){
            List<Tag> newTagList = new ArrayList<>();
            if(CollectionUtil.isEmpty(p.getEditedNewTags())){
                try {
                    newTagList = tagService.createNewTags(
                            p.getEditedNewTags().stream()
                                    .filter(StringUtil::isNotBlank)
                                    .collect(Collectors.toSet()),
                            userUid
                    );
                } catch (TagLimitExceededException e) {
                    log.warn("User {} tag limit exceeded, skipping new tags", userUid);
                }
            }
            p.setTitle(p.getEditedTitle());
            p.setSubtitle(p.getEditedSubtitle());
            p.setContent(p.getEditedContent());
            p.setSummary(p.getEditedSummary());
            if(p.getEditedNewTags() != null){
                List<String> newTagNames = p.getEditedNewTags();
                p.setTags(Stream.concat(
                                newTagList.stream(),
                                p.getTags().stream()
                                        .filter(t -> !newTagNames.contains(t.getName())
                        )
                ).toList());
            }
            if(p.getEditedTagIds() != null){
                List<Tag> editedTags = tagRepository.findAllById(p.getEditedTagIds());
                List<Tag> oldTags = p.getTags();
                List<Tag> allTags = Stream.concat(
                                newTagList.stream(),
                                editedTags.stream()
                        )
                        .collect(Collectors.toMap(
                                Tag::getId,
                                tag -> tag,
                                (existing, replacement) -> existing
                        ))
                        .values()
                        .stream()
                        .toList();

                List<Long> newTagIds = allTags.stream().map(Tag::getId).toList();
                List<Long> removedTagIds = oldTags.stream().filter(t-> ! editedTags.contains(t)).map(Tag::getId).toList();

                tagRepository.increaseUsageCountInIds(newTagIds, 1);
                tagRepository.decreaseUsageCountInIds(removedTagIds, 1);
                p.setTags(allTags);
            }
            categoryRepository.increaseUsageCountById(p.getEditedCategory().getId(), 1);
            if(p.getCategory() != null){
                if(!Objects.equals(p.getCategory().getId(), p.getEditedCategory().getId())){
                    categoryRepository.decreaseUsageCountById(p.getCategory().getId(), 1);
                }
            }
            p.setCategory(p.getEditedCategory());

            p.setVersion(p.getVersion() + 1);
            p.setStatus(PostStatus.PUBLISHED);
            p.setPublishedAt(Instant.now());
            postRepository.save(p);
        } else {
            p.setStatus(PostStatus.REJECTED);
            postRepository.save(p);
        }
    }

    @Transactional
    @Override
    public void handleBatch(List<ModerationConsumerMessage> msgs) {
        if (msgs.isEmpty()) return;
        Map<TargetType, List<ModerationConsumerMessage>> byType = msgs.stream()
                .collect(Collectors.groupingBy(ModerationConsumerMessage::getTargetType));
        byType.forEach((type, typeMsgs) -> {
            switch (type) {
                case POST -> handlePostBatch(typeMsgs);
                case POST_CONTENT_IMAGE -> handlePostContentImageBatch(typeMsgs);
                case POST_COVERAGE_IMAGE -> handlePostCoverageImageBatch(typeMsgs);
                default -> log.warn("Unhandled batch target type in batch: {}", type);
            }
        });

        moderationInboxHandler.handleBatch(msgs);
    }

    private void handlePostCoverageImageBatch(List<ModerationConsumerMessage> typeMsgs) {
        log.warn("Post coverage image moderation batch handling not implemented yet. Message count: {}", typeMsgs.size());
    }

    private void handlePostContentImageBatch(List<ModerationConsumerMessage> typeMsgs) {
        log.warn("Post content image moderation batch handling not implemented yet. Message count: {}", typeMsgs.size());
    }

    @Transactional
    public void handlePostBatch(List<ModerationConsumerMessage> msgs) {
        if (msgs.isEmpty()) return;
        // Group by status
        Map<AuditStatus, List<ModerationConsumerMessage>> byStatus = msgs.stream()
                .collect(Collectors.groupingBy(ModerationConsumerMessage::getStatus));
        // Query for all tasks
        List<Long> allTaskIds = msgs.stream()
                .map(ModerationConsumerMessage::getId)
                .distinct()
                .toList();
        Map<Long, AuditTask> taskMap = auditTaskRepository.findAllById(allTaskIds)
                .stream()
                .collect(Collectors.toMap(AuditTask::getId, Function.identity()));
        // Query for all posts by targetId(postId)
        List<Long> allPostIds = taskMap.values().stream()
                .map(t -> Long.valueOf(t.getTargetId()))
                .distinct()
                .toList();
        Map<Long, Post> postMap = postRepository.findAllByIdInAndIsDeletedAndStatus(
                        allPostIds, false, PostStatus.PENDING
                )
                .stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        // Process by group
        byStatus.forEach((status, messages) -> {
            List<Post> posts = messages.stream()
                    .map(m -> taskMap.get(m.getId()))
                    .filter(Objects::nonNull)
                    .map(t -> postMap.get(Long.valueOf(t.getTargetId())))
                    .filter(Objects::nonNull)
                    .toList();

            if (status == AuditStatus.APPROVED) {
                handlePostApprovedBatch(posts);
            } else if (status == AuditStatus.REJECTED) {
                posts.forEach(p -> p.setStatus(PostStatus.REJECTED));
                postRepository.saveAll(posts);;
            }
        });
        moderationInboxHandler.handleBatch(msgs);
    }

    private void handlePostApprovedBatch(List<Post> posts) {
        // collection all new tag names by postId
        Map<Long, Set<String>> authorToTagNames = new HashMap<>();
        posts.forEach(p -> {
            if (CollectionUtil.isNotEmpty(p.getEditedNewTags())) {
                Long authorUid = p.getAuthor().getUid();
                Set<String> names = p.getEditedNewTags().stream()
                        .filter(StringUtil::isNotBlank)
                        .collect(Collectors.toSet());
                authorToTagNames.merge(authorUid, names, (a, b) -> {
                    a.addAll(b);
                    return a;
                });
            }
        });
        // batch create new tags and collection all new tags by name
        Map<String, Tag> allNewTags = new ConcurrentHashMap<>();
        authorToTagNames.forEach((authorUid, tagNames) -> {
            try {
                List<Tag> created = tagService.createNewTags(tagNames, authorUid);
                created.forEach(t -> allNewTags.put(t.getName(), t));
            } catch (TagLimitExceededException e) {
                log.warn("User {} tag limit exceeded, skipping new tags", authorUid);
            }
        });
        // get all exists tags
        Set<Long> allExistTagIds = posts.stream()
                .flatMap(p -> p.getEditedTagIds().stream())
                .collect(Collectors.toSet());
        Map<Long, Tag> existTagMap = tagRepository.findAllById(allExistTagIds)
                .stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));

        posts.forEach(p -> {
            List<Tag> newTags = p.getEditedNewTags().stream()
                    .filter(StringUtil::isNotBlank)
                    .map(allNewTags::get)
                    .filter(Objects::nonNull)
                    .toList();

            List<Tag> existTags = p.getEditedTagIds().stream()
                    .map(existTagMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            List<Tag> allTags = Stream.concat(newTags.stream(), existTags.stream())
                    .distinct()
                    .toList();

            p.setTitle(p.getEditedTitle());
            p.setSubtitle(p.getEditedSubtitle());
            p.setContent(p.getEditedContent());
            p.setSummary(p.getEditedSummary());
            p.setCategory(p.getEditedCategory());
            p.setTags(allTags);
            p.setVersion(p.getVersion() + 1);
            p.setStatus(PostStatus.PUBLISHED);
        });

        postRepository.saveAll(posts);
    }

}
