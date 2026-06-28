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
import org.waterwood.waterfunservicecore.entity.post.PostEditStatus;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.TagLimitExceededException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
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
    private final ResourceRepository resourceRepository;

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
        Post p = postRepository.findByIdAndIsDeleted(
                Long.valueOf(task.getTargetId()),
                false
        ).orElseThrow(() -> new IllegalStateException("Post not found for id: " + task.getTargetId()));
        if (p.getStatus() != PostStatus.PENDING && p.getEditStatus() != PostEditStatus.PENDING) {
            throw new IllegalStateException("Post not in pending state for id: " + task.getTargetId());
        }
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
            if(CollectionUtil.isNotEmpty(p.getEditedNewTags())){
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

            // Snapshot original tags from persistent state before any modifications
            Set<Long> originalTagIdSet = p.getTags().stream().map(Tag::getId).collect(Collectors.toSet());

            if(p.getEditedTagIds() != null){
                List<Tag> editedTags = tagRepository.findAllById(p.getEditedTagIds());
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

                Set<Long> finalTagIdSet = allTags.stream().map(Tag::getId).collect(Collectors.toSet());
                List<Long> removedTagIds = originalTagIdSet.stream()
                        .filter(id -> !finalTagIdSet.contains(id))
                        .toList();
                List<Long> addedTagIds = finalTagIdSet.stream()
                        .filter(id -> !originalTagIdSet.contains(id))
                        .toList();

                if (!addedTagIds.isEmpty()) tagRepository.increaseUsageCountInIds(addedTagIds, 1);
                if (!removedTagIds.isEmpty()) tagRepository.decreaseUsageCountInIds(removedTagIds, 1);
                p.setTags(allTags);
            } else if (CollectionUtil.isNotEmpty(newTagList)) {
                // Only new tags, no existing edited tags
                Set<Long> finalTagIdSet = newTagList.stream().map(Tag::getId).collect(Collectors.toSet());
                List<Long> addedTagIds = finalTagIdSet.stream()
                        .filter(id -> !originalTagIdSet.contains(id))
                        .toList();
                if (!addedTagIds.isEmpty()) tagRepository.increaseUsageCountInIds(addedTagIds, 1);
                p.setTags(newTagList);
            }
            if(p.getCategory() == null){
                categoryRepository.increaseUsageCountById(p.getEditedCategory().getId(), 1);
            } else if(!Objects.equals(p.getCategory().getId(), p.getEditedCategory().getId())){
                categoryRepository.decreaseUsageCountById(p.getCategory().getId(), 1);
                categoryRepository.increaseUsageCountById(p.getEditedCategory().getId(), 1);
            }
            p.setCategory(p.getEditedCategory());

            promoteCoverImage(p);
            p.setEditStatus(PostEditStatus.NONE);

            p.setVersion(p.getVersion() + 1);
            p.setStatus(PostStatus.PUBLISHED);
            p.setPublishedAt(Instant.now());
            postRepository.save(p);
        } else {
            if (p.getEditStatus() == PostEditStatus.PENDING) {
                // Re-edit rejection: mark as REJECTED so user knows, keep edited fields for resubmission
                p.setEditStatus(PostEditStatus.REJECTED);
            } else {
                // New post rejection
                p.setStatus(PostStatus.REJECTED);
            }
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
        List<Post> allFound = postRepository.findAllById(allPostIds);
        Map<Long, Post> postMap = allFound.stream()
                .filter(p -> Boolean.FALSE.equals(p.getIsDeleted()))
                .filter(p -> p.getStatus() == PostStatus.PENDING || p.getEditStatus() == PostEditStatus.PENDING)
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
                posts.forEach(p -> {
                    if (p.getEditStatus() == PostEditStatus.PENDING) {
                        p.setEditStatus(PostEditStatus.REJECTED);
                    } else {
                        p.setStatus(PostStatus.REJECTED);
                    }
                });
                postRepository.saveAll(posts);
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
        // batch create new tags using pre-fetched tag counts to avoid N+1
        Map<Long, Integer> tagCounts = new HashMap<>();
        if (!authorToTagNames.isEmpty()) {
            List<Object[]> counts = tagRepository.countByCreatorUidIn(new ArrayList<>(authorToTagNames.keySet()));
            for (Object[] row : counts) {
                tagCounts.put((Long) row[0], ((Number) row[1]).intValue());
            }
        }
        Map<String, Tag> allNewTags = new ConcurrentHashMap<>();
        authorToTagNames.forEach((authorUid, tagNames) -> {
            try {
                int existingCount = tagCounts.getOrDefault(authorUid, 0);
                List<Tag> created = tagService.createNewTags(tagNames, authorUid, existingCount);
                created.forEach(t -> allNewTags.put(t.getName(), t));
            } catch (TagLimitExceededException e) {
                log.warn("User {} tag limit exceeded, skipping new tags", authorUid);
            }
        });
        // get all exists tags
        Set<Long> allExistTagIds = posts.stream()
                .filter(p -> p.getEditedTagIds() != null)
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

            List<Tag> existTags = p.getEditedTagIds() != null
                    ? p.getEditedTagIds().stream()
                            .map(existTagMap::get)
                            .filter(Objects::nonNull)
                            .toList()
                    : List.of();

            List<Tag> allTags = Stream.concat(newTags.stream(), existTags.stream())
                    .distinct()
                    .toList();

            // Usage count: compare original vs final
            Set<Long> originalTagIdSet = p.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
            Set<Long> finalTagIdSet = allTags.stream().map(Tag::getId).collect(Collectors.toSet());
            List<Long> addedTagIds = finalTagIdSet.stream()
                    .filter(id -> !originalTagIdSet.contains(id))
                    .toList();
            List<Long> removedTagIds = originalTagIdSet.stream()
                    .filter(id -> !finalTagIdSet.contains(id))
                    .toList();
            if (!addedTagIds.isEmpty()) tagRepository.increaseUsageCountInIds(addedTagIds, 1);
            if (!removedTagIds.isEmpty()) tagRepository.decreaseUsageCountInIds(removedTagIds, 1);

            p.setTitle(p.getEditedTitle());
            p.setSubtitle(p.getEditedSubtitle());
            p.setContent(p.getEditedContent());
            p.setSummary(p.getEditedSummary());
            if(p.getCategory() == null){
                categoryRepository.increaseUsageCountById(p.getEditedCategory().getId(), 1);
            } else if(!Objects.equals(p.getCategory().getId(), p.getEditedCategory().getId())){
                categoryRepository.decreaseUsageCountById(p.getCategory().getId(), 1);
                categoryRepository.increaseUsageCountById(p.getEditedCategory().getId(), 1);
            }
            p.setCategory(p.getEditedCategory());
            p.setTags(allTags);
            promoteCoverImage(p);
            p.setEditStatus(PostEditStatus.NONE);
            p.setVersion(p.getVersion() + 1);
            p.setStatus(PostStatus.PUBLISHED);
        });

        postRepository.saveAll(posts);
    }

    private void promoteCoverImage(Post p) {
        String coverUuid = p.getEditedCoverImg();
        if (StringUtil.isNotBlank(coverUuid)) {
            Resource newRes = resourceRepository.findByUuidAndStatusNot(coverUuid, ResourceStatus.DELETED)
                    .orElse(null);
            if (newRes != null) {
                Resource oldRes = p.getCoverageResource();
                if (oldRes != null && !oldRes.getUuid().equals(coverUuid)) {
                    oldRes.setStatus(ResourceStatus.ORPHAN);
                }
                newRes.setStatus(ResourceStatus.ACTIVE);
                p.setCoverageResource(newRes);
            }
        } else {
            Resource oldRes = p.getCoverageResource();
            if (oldRes != null) {
                oldRes.setStatus(ResourceStatus.ORPHAN);
                p.setCoverageResource(null);
            }
        }
    }
}
