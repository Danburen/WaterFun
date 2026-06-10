package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.UserBizType;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservice.service.NotificationService;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservice.api.response.post.*;
import org.waterwood.waterfunservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunservice.service.post.TagService;
import org.waterwood.waterfunservicecore.api.moderation.PostAuditPayload;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.entity.post.PostResource;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.post.*;
import org.waterwood.waterfunservicecore.entity.resource.*;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.UserCollectExceedLimitException;
import org.waterwood.waterfunservicecore.exception.notfound.PostNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.UserAssociationDataNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.CategoryReferenceInvalidException;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.*;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserCounterRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservice.service.post.PostService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.infrastructure.validation.UploadValidator;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;
import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.services.audit.UserActivityLogService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.ContentIdGenerator;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final IdentifierGenerator identifierGenerator;
    private final UserCoreService userCoreService;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final CloudFileService cloudFileService;
    private final PostMapper postMapper;
    private final MessageSource messageSource;
    private final AuditTaskRepository auditTaskRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final TagService tagService;
    private final PostResourceRepository postResourceRepository;
    private final UserBriefService userBriefService;
    private final UserCounterRepository userCounterRepository;
    private final UserLikeRepository userLikeRepository;
    private final UserCollectRepository userCollectRepository;
    private final NotificationService notificationService;
    private final SiteStatisticRecorder siteStatisticRecorder;
    private final UserActivityLogService userActivityLogService;

    @Value("${user.quota.collect:10000}")
    private Long userCollectExceedLimit;

    @Override
    public void add(Post post, Set<Long> tagIds) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        post.setAuthor(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        post.setSlug(identifierGenerator.generateSlug(post.getTitle(), postRepository));
        post.setTags(tags);
        postRepository.save(post);
        userActivityLogService.record(post.getAuthor().getUid(), UserActionType.CREATE, BusinessType.POST, post.getId());
    }

    @Override
    public Page<Post> listPosts(Specification<Post> spec, Pageable pageable) {
        return postRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post p = postRepository.getReferenceById(id);
        if(Objects.equals(p.getAuthor().getUid(), UserCtxHolder.getUserUid())) {
            postRepository.deleteById(id);
            userActivityLogService.record(UserCtxHolder.getUserUid(), UserActionType.DELETED, BusinessType.POST, id);
        }else{
            throw new BizException(BaseResponseCode.FORBIDDEN);
        }
    }

    @Override
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(PostNotFoundException::new);
    }

    @Override
    public Page<PostCardResp> listCardPosts(Specification<Post> spec, Pageable pageable) {
        return listCardPostsInternal(
                spec,
                pageable,
                postMapper::toPostCardResponseDto,
                (res,
                 post,
                 postTagMap,
                 postCategoryMap,
                 postCoverageImgMap,
                 postUserBriefMap
                ) -> {
                    Long postId = post.getId();
                    res.setTags(postTagMap.getOrDefault(postId, Collections.emptyList()));
                    res.setCategory(postCategoryMap.get(postId));
                    res.setCoverImage(postCoverageImgMap.get(postId));
                    res.setUserBrief(postUserBriefMap.get(postId));
                }
        );
    }

    @Override
    public Page<PostAuthorCardResp> listAuthorCardPosts(Specification<Post> spec, Pageable pageable) {
        return listCardPostsInternal(
                spec,
                pageable,
                postMapper::toPostAuthorCardResp,
                (res,
                 post,
                 postTagMap,
                 postCategoryMap,
                 postCoverageImgMap,
                 postUserBriefMap
                ) -> {
                    Long postId = post.getId();
                    res.setTags(postTagMap.getOrDefault(postId, Collections.emptyList()));
                    res.setCategory(postCategoryMap.get(postId));
                    res.setCoverImage(postCoverageImgMap.get(postId));
                }
        );
    }

    @Override
    public PostDetailResp getPostDetail(Long id) {
        return postRepository.findByIdAndVisibilityAndIsDeleted(id, PostVisibility.PUBLIC, false)
                .map(post -> buildPostDetailResp(
                        post,
                        postMapper::toPostDetailResp,
                        (res, tags, category, coverImg) -> {
                            res.setTags(tags);
                            res.setCategory(category);
                            res.setCoverImage(coverImg);
                        }
                ))
                .orElseThrow(() -> new BizException(BaseResponseCode.NOT_FOUND));
    }

    @Override
    public PostAuthorDetailResp getSelfPostDetail(Long id) {
        return postRepository.findByIdAndAuthorUidAndIsDeleted(id, UserCtxHolder.getUserUid(), false)
                .map(post -> buildPostDetailResp(
                        post,
                        postMapper::toPostAuthorDetailResp,
                        (res, tags, category, coverImg) -> {
                            res.setTags(tags);
                            res.setCategory(category);
                            res.setCoverImage(coverImg);
                        }
                ))
                .orElseThrow(() -> new BizException(BaseResponseCode.NOT_FOUND));
    }

    @Override
    public Long draftNew() {
        Long id = ContentIdGenerator.nextPostId();
        Post p = new Post();
        p.setId(id);
        p.setEditedTitle(messageSource.getMessage(
                "post.title.draft.untitled",
                null,
                "Untitled Post",
                UserCtxHolder.getLocale()));
        p.setContent("");
        p.setTitle("");
        p.setEditedContent("");
        p.setAuthor(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        postRepository.save(p);
        siteStatisticRecorder.recordNewPost();
        return id;
    }

    @Transactional
    @Override
    public void publish(Long id, PostSaveReq req) {
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                id, UserCtxHolder.getUserUid(), false
        ).orElseThrow(PostNotFoundException::new);
        this.publish(p, req);
    }

    @Transactional
    @Override
    public void publishNewPost(PostSaveReq req) {
        Long id = ContentIdGenerator.nextPostId();
        Post p = new Post();
        p.setId(id);
        p.setAuthor(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        publish(p, req);
    }

    @Transactional
    @Override
    public void saveNewPost(PostSaveReq req) {
        Long id = ContentIdGenerator.nextPostId();
        Post p = new Post();
        p.setId(id);
        p.setAuthor(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        save(p, req);
    }

    @Transactional
    @Override
    public void save(Long id, PostSaveReq request) {
        Post p = postRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(NotFoundException::new);
        this.save(p, request);
    }

    /**
     * Public a post
     * @param p {@link Post} post entity, must contain id, not essentially exist in db
     * @param req {@link PostSaveReq}
     */
    @Transactional
    protected void publish(Post p, PostSaveReq req) {
        // save the resource and form and synchronize the resources.
        this.save(p, req);
        // Preparing for audit task
        String targetId = p.getId().toString();
        AuditTask task = auditTaskRepository
                .findByTargetIdAndTargetTypeAndStatus(targetId, TargetType.POST, AuditStatus.PENDING)
                .orElseGet(() -> {
                    AuditTask newTask = new AuditTask();
                    newTask.setTargetId(targetId);
                    newTask.setSubmitAt(Instant.now());
                    newTask.setTargetType(TargetType.POST);
                    newTask.setSubmitter(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
                    return newTask;
                });

        task.setStatus(AuditStatus.PENDING);
        task.setUserLocale(UserCtxHolder.getLocale().getLanguage());
        task.setContentFormat(AuditContentFormat.MARKDOWN); // todo suppose determine the content type
        task.setContent(p.getEditedContent());
        task.setPayload(new PostAuditPayload(
                p.getEditedTitle(),
                p.getEditedSubtitle(),
                p.getEditedContent(),
                p.getEditedSummary(),
                p.getEditedCoverImg(),
                p.getEditedCategory() != null ? p.getEditedCategory().getId() : null,
                p.getEditedTagIds(),
                p.getEditedNewTags(),
                null
        ).toJson());

        p.setStatus(PostStatus.PENDING);
        postRepository.save(p);
        task = auditTaskRepository.save(task);
        // Collect all the resource include content image and coverage
        Set<String> allUuids = new HashSet<>(StringUtil.extraResPlaceholders(p.getEditedContent()));
        List<String> linkedResourceUuids = getUnDeletedLinkedResourceUuids(p.getId());
        if (StringUtil.isNotBlank(p.getEditedCoverImg())) {
            allUuids.add(p.getEditedCoverImg());
        }
        AuditTask finalTask = task;
        List<AuditResource> auditResources = resourceRepository
                .findByUuidInAndStatus(allUuids, ResourceStatus.ACTIVE)
                .stream()
                .filter(r -> linkedResourceUuids.contains(r.getUuid())
                        || r.getUuid().equals(p.getEditedCoverImg()))
                .map(res -> {
                    AuditResource ar = new AuditResource();
                    AuditResourceId arId = new AuditResourceId();
                    arId.setResourceUuid(res.getUuid());
                    arId.setTaskId(finalTask.getId());
                    ar.setId(arId);
                    ar.setTask(finalTask);
                    ar.setResource(res);
                    return ar;
                })
                .toList();
        Set<String> existingAuditResUuids = auditTaskResourceRepository
                .findByTaskId(task.getId())
                .stream()
                .map(ar -> ar.getResource().getUuid())
                .collect(Collectors.toSet());

        List<AuditResource> toSave = auditResources.stream()
                .filter(ar -> !existingAuditResUuids.contains(ar.getResource().getUuid()))
                .toList();

        if (!toSave.isEmpty()) {
            auditTaskResourceRepository.saveAll(toSave);
        }
    }

    /**
     * Save a post
     * @param p {@link Post} post entity, must contain id, not essentially exist in db
     * @param req {@link PostSaveReq}
     */
    @Transactional
    public void save(Post p, PostSaveReq req) {
        if (!p.getAuthor().getUid().equals(UserCtxHolder.getUserUid())) {
            throw new ForbiddenException();
        }
        p.setEditedTitle(req.getTitle());
        p.setEditedSubtitle(req.getSubtitle());

        syncResource(p, p.getEditedContent(), req.getContent());
        p.setEditedContent(req.getContent());
        p.setEditedSummary(req.getSummary());
        // Coverage
        String oldCoverageImgId = p.getEditedCoverImg();
        String newCoverageImgId = req.getCoverageImgId();
        boolean needReplace = StringUtil.isNotBlank(newCoverageImgId) && !newCoverageImgId.equals(oldCoverageImgId)
                || StringUtil.isBlank(newCoverageImgId) && StringUtil.isNotBlank(oldCoverageImgId);
        if (needReplace && StringUtil.isNotBlank(oldCoverageImgId)) {
            resourceRepository.findByUuidAndStatusNot(oldCoverageImgId, ResourceStatus.DELETED)
                    .ifPresent(oldRes -> {
                        oldRes.setStatus(ResourceStatus.ORPHAN);
                        resourceRepository.save(oldRes);

                        final String oldKey = oldRes.getResourceKey();
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        try {
                                            // todo supposed to be after completion of whole transaction including audit task creation
                                            cloudFileService.removeFile(CloudFSRoot.UPLOADS, oldKey);
                                        } catch (Exception e) {
                                            log.error("Failed to remove old cloud file: {}", oldKey, e);
                                        }
                                    }
                                }
                        );
                    });
        }
        if (StringUtil.isNotBlank(newCoverageImgId)) {
            Resource res = resourceRepository.findByUuidAndStatusNot(newCoverageImgId, ResourceStatus.DELETED)
                    .orElseThrow(() -> new ResourceReferenceInvalidException(newCoverageImgId));
            res.setStatus(ResourceStatus.ACTIVE);
            resourceRepository.save(res);
            p.setEditedCoverImg(res.getUuid());
        } else {
            p.setEditedCoverImg(null);
        }

        Category c = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new CategoryReferenceInvalidException(req.getCategoryId()));
        p.setEditedCategory(c);
        // Below should be created when public, and must after audition, not in temporary saving.
        // List<Tag> newTagCreated = tagService.createNewTags(request.getNewTags(), UserCtxHolder.getUserUid());
        if(req.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(req.getTagIds());
            p.setEditedTagIds(tags.stream().map(Tag::getId).toList());
        }
        p.setEditedNewTags(req.getNewTags().stream()
                .map(str -> str.trim().replaceAll("[^a-z0-9-]", ""))
                .toList());
        postRepository.save(p);
    }

    /**
     * Synchronize resource for given content for a post
     * only those resource in the new content linked by {@link PostResource} or associated with author will be synchronized.
     * must run in transactional
     *
     * @param p             target post id to identify whether new content resource belongs to the post.
     * @param originContent original content
     * @param newContent    new content
     */
    private void syncResource(Post p, String originContent, String newContent) {
        // 1. Extract resource UUIDs from content placeholders
        Set<String> originResUuids = StringUtil.extraResPlaceholders(originContent);;
        Set<String> newResUuids = StringUtil.extraResPlaceholders(newContent);
        // Filter new resources: must be either already linked to this post,
        // or uploaded by the author and currently ACTIVE
        List<String> postAvailableResources = getUnDeletedLinkedResourceUuids(p.getId());
        Set<String> linkedNewResUuids = newResUuids.stream()
                .filter(postAvailableResources::contains)
                .collect(Collectors.toSet());
        Set<String> unlinkedNewResUuids = newResUuids.stream()
                .filter(uuid -> !postAvailableResources.contains(uuid))
                .collect(Collectors.toSet());
        // Author's own active resources that are referenced in new content
        List<Resource> authorResources = resourceRepository.findByUploaderIdAndUuidInAndStatus(
                p.getAuthor().getUid(), unlinkedNewResUuids, ResourceStatus.ACTIVE
        );

        savePostResource(p, authorResources);
        Set<String> allValidNewResUuids = new HashSet<>(linkedNewResUuids);
        allValidNewResUuids.addAll(authorResources.stream().map(Resource::getUuid).toList());
        if (originResUuids.equals(allValidNewResUuids)) {
            return;
        }
        Set<String> toOrphanUuids = new HashSet<>(originResUuids);
        toOrphanUuids.removeAll(allValidNewResUuids);
        Set<String> toActivateUuids = new HashSet<>(linkedNewResUuids);
        toActivateUuids.removeAll(originResUuids);
        if (!toActivateUuids.isEmpty()) {
            resourceRepository.batchUpdateStatusFromTo(ResourceStatus.ORPHAN, ResourceStatus.ACTIVE, toActivateUuids);
        }
        if (!toOrphanUuids.isEmpty()) {
            resourceRepository.batchUpdateStatusFromTo(ResourceStatus.ACTIVE, ResourceStatus.ORPHAN, toOrphanUuids);
        }
    }

    private void savePostResource(Post p, List<Resource> resources) {
        Set<String> existingUuids = postResourceRepository
                .findByPostId(p.getId())
                .stream()
                .map(pr -> pr.getResourceUuid().getUuid())
                .collect(Collectors.toSet());

        List<PostResource> toSave = resources.stream()
                .filter(res -> !existingUuids.contains(res.getUuid()))
                .map(res -> {
                    PostResource pr = new PostResource();
                    PostResourceId prId = new PostResourceId();
                    prId.setResourceUuid(res.getUuid());
                    prId.setPostId(p.getId());
                    pr.setId(prId);
                    pr.setPost(p);
                    pr.setResourceUuid(res);
                    return pr;
                })
                .toList();

        if (!toSave.isEmpty()) {
            postResourceRepository.saveAll(toSave);
        }
    }

    private List<String> getUnDeletedLinkedResourceUuids(Long postId) {
        return postResourceRepository
                .findByPostIdAndResourceUuidStatusNot(postId, ResourceStatus.DELETED)
                .stream().map(pr -> pr.getResourceUuid().getUuid())
                .toList();
    }

    @Override
    public String contentPreview(Long id, String content) {
        if(content.isBlank()) return "";
        long userUid = UserCtxHolder.getUserUid();
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                id, userUid, false
        ).orElseThrow(PostNotFoundException::new);

        Set<String> resourceUuids = StringUtil.extraResPlaceholders(content);
        List<PostResource> postResources = postResourceRepository.findAllByPostIdAndResourceUuidUuidIn(
                p.getId(), resourceUuids
        );

        Map<String, String> resourceMap = postResources.stream()
                .collect(Collectors.toMap(
                        pr -> pr.getResourceUuid().getUuid(),
                        pr -> pr.getResourceUuid().getResourceKey()
                ));

        Map<String, CloudResPresignedUrlResp> respMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                resourceMap,
                TargetType.POST_CONTENT_IMAGE
        );

        return StringUtil.replaceResPlaceholders(
                content,
                respMap.entrySet().stream()
                        .filter(e -> e.getValue() != null && e.getValue().getUrl() != null)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().getUrl()
                        ))
        );
    }

    @Override
    public PostDraftResp getEditPostDraft(Long id) {
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                id, UserCtxHolder.getUserUid(), false
        ).orElseThrow(PostNotFoundException::new);

        PostDraftResp resp = postMapper.toPostDraftResp(p);
        resp.setCoverageImgPresignedUrl(p.getEditedCoverImg() == null ? null :
                cloudFileService.getReadUrlCached(
                        CloudFSRoot.UPLOADS,
                        p.getEditedCoverImg(),
                        p.getId(),
                        TargetType.POST_CONTENT_IMAGE
                )
        );

        if(p.getEditedCategory() != null) {
            resp.setEditedCategoryId(OptionVO.<Long>builder()
                    .id(p.getEditedCategory().getId())
                    .name(p.getEditedCategory().getName())
                    .build());
        }
        if( p.getEditedTagIds() != null) {
            List<OptionVO<Long>> editedTags = tagRepository.findTagOptionVOsByPostIdIn(p.getEditedTagIds());

            resp.setEditedTagIds(editedTags);
        }
        if(p.getEditedNewTags() != null) {
            resp.setEditedNewTagIds(p.getEditedNewTags());
        }

        return resp;
    }

    @Transactional
    @Override
    public void like(Long postId) {
        Long userUid = UserCtxHolder.getUserUid();
        PostAuthorUidTitleDO pdo = postRepository.findPostAuthorIdTitleDOById(
                postId
        ).orElse(null);
        userLikeRepository.findById(new UserLikeId(userUid, postId))
                .ifPresentOrElse(
                        like -> {
                            userCounterRepository.decreaseUserLikeCount(userUid, 1);
                            postRepository.decreaseLikeCount(postId, 1);
                            userLikeRepository.delete(like);
                            userActivityLogService.record(userUid, UserActionType.DELETED, BusinessType.POST, postId);
                        },
                        () -> {
                            UserLike newLike = new UserLike();
                            newLike.setId(new UserLikeId(userUid, postId));
                            userLikeRepository.save(newLike);
                            userCounterRepository.increaseUserLikeCount(userUid, 1);
                            postRepository.increaseLikeCount(postId, 1);
                            userActivityLogService.record(userUid, UserActionType.CREATE, BusinessType.POST, postId);
                            if(pdo != null) {
                                notificationService.onPostLike(
                                        pdo.getAuthorUid(),
                                        userUid,
                                        postId,
                                        pdo.getTitle(),
                                        pdo.getCoverageResourceUuid()
                                );
                            }
                        }
                );
    }

    @Override
    public void collection(Long postId) {
        Long userUid = UserCtxHolder.getUserUid();
        UserCounter uc = userCounterRepository.findByUserUid(userUid)
                .orElseThrow(UserAssociationDataNotFoundException::new);
        PostAuthorUidTitleDO pdo = postRepository.findPostAuthorIdTitleDOById(
                postId
        ).orElse(null);
        userCollectRepository.findById(new UserCollectId(userUid, postId))
                .ifPresentOrElse(
                        collection -> {
                            userCounterRepository.decreaseUserCollectionCount(userUid, 1);
                            postRepository.decreaseCollectCount(postId, 1);
                            userCollectRepository.delete(collection);
                            userActivityLogService.record(userUid, UserActionType.DELETED, BusinessType.POST, postId);
                        },
                        () -> {
                            if (uc.getCollectCnt() >= userCollectExceedLimit) {
                                throw new UserCollectExceedLimitException();
                            }
                            UserCollect newCollection = new UserCollect();
                            newCollection.setId(new UserCollectId(userUid, postId));
                            userCounterRepository.increaseUserCollectionCount(userUid, 1);
                            postRepository.increaseCollectCount(postId, 1);
                            userCollectRepository.save(newCollection);
                            userActivityLogService.record(userUid, UserActionType.CREATE, BusinessType.POST, postId);
                            if(pdo != null) {
                                notificationService.onPostCollect(
                                        pdo.getAuthorUid(),
                                        userUid,
                                        postId,
                                        pdo.getTitle(),
                                        pdo.getCoverageResourceUuid()
                                );
                            }
                        }
                );

    }

    @Transactional
    @Override
    public List<PresignedResp> handlePostCoverageImageUpload(UserUploadPolicyReq request) {
        Long bizId;
        boolean isLinkToPost = false;
        if(request.getBizId() == null){
            bizId = UserCtxHolder.getUserUid();
        } else {
            bizId = Long.parseLong(request.getBizId());
            isLinkToPost = true;
        }
        FileExtension ext =  UploadValidator.validateSingleFileUpload(request, TargetType.POST_COVERAGE_IMAGE);

        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = BizUploadPayload.of(
                bizId,
                request.getBizType().getCode(),
                resourceUUID
        );
        String cosPath = CosKeyPathGenerator.of(resourceUUID, ext);

        Resource res = new Resource();
        res.setUuid(StringUtil.noDashUUIDString(resourceUUID));
        res.setResourceKey(cosPath);
        res.setUploaderId(UserCtxHolder.getUserUid());
        res.setSourceType(SourceType.USER_UPLOADED);

        resourceRepository.save(res);
        if(isLinkToPost){
            Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                    bizId, UserCtxHolder.getUserUid(),false
            ).orElseThrow(PostNotFoundException::new);

            PostResource postResource = new PostResource();
            PostResourceId postResourceId = new PostResourceId();
            postResourceId.setResourceUuid(res.getUuid());
            postResourceId.setPostId(p.getId());

            postResource.setId(postResourceId);
            postResource.setResourceUuid(res);
            postResource.setPost(p);
            postResourceRepository.save(postResource);
        }

        return List.of(cloudFileService.buildPutPolicyForUploads(cosPath, payload));
    }

    @Transactional
    @Override
    public List<PresignedResp> handlePostContentImageUpload(UserUploadPolicyReq request) {
        Long bizId;
        boolean isLinkToPost = false;
        if(StringUtil.isBlank(request.getBizId())){
            bizId = UserCtxHolder.getUserUid();
        } else {
            bizId = Long.parseLong(request.getBizId());
            isLinkToPost = true;
        }

        List<PresignedResp> results = new ArrayList<>();
        List<UploadItem> validItems = new ArrayList<>();

        List<FileExtension> exts = request.getExts().stream().map(FileExtension::fromExt).toList();
        for(int i = 0; i < exts.size(); i++){
            if (TargetType.POST_CONTENT_IMAGE.isAllowed(exts.get(i))) {
                UUID uuid = UUID.randomUUID();
                results.add(null);
                validItems.add(new UploadItem(
                        i,
                        CosKeyPathGenerator.of(uuid, exts.get(i)),
                        StringUtil.noDashUUIDString(uuid),
                        uuid,
                        exts.get(i))
                );
            }else{
                results.add(PresignedResp.ofError("system.file_type_not_allowed"));
            }
        }

        if(! validItems.isEmpty()){
            List<Resource> resources = validItems.stream()
                    .map(item -> {
                        return cloudFileService.createAndSetUpUploadRes(item.uuidPlain, item.path, UserCtxHolder.getUserUid());
                    })
                    .toList();
            resourceRepository.saveAll(resources);
            if(isLinkToPost){
                Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                        bizId, UserCtxHolder.getUserUid(),false
                ).orElseThrow(PostNotFoundException::new);
                savePostResource(p, resources);
            }

            List<BizUploadPayload> payloads = validItems.stream()
                    .map(item -> BizUploadPayload.of(
                            bizId,
                            UserBizType.POST_CONTENT_IMAGE.name(),
                            item.uuid()
                    ))
                    .toList();
            List<PresignedResp> signed = cloudFileService.batchBuildPutPolicyForUploads(
                    validItems.stream().map(UploadItem::path).toList(),
                    payloads
            );
            for (int i = 0; i < signed.size(); i++) {
                int originalIndex = validItems.get(i).originalIndex();
                results.set(originalIndex, signed.get(i));
            }
        }
        return results;
    }

    @Transactional
    @Override
    public void handlePostImageUploadCallback(CloudPutCallbackReq request, UserUploadContext<Long> ctx) {
        Assert.isTrue(ctx.getBizType() == UserBizType.POST_COVERAGE_IMAGE
                        || ctx.getBizType() == UserBizType.POST_CONTENT_IMAGE,
                "Invalid biz type: " + ctx.getBizType()
        );

        Long postId = ctx.getBizId();
        String resourceUuid = ctx.getResourceUuid();
        postRepository.findByIdAndAuthorUidAndIsDeleted(
                postId, UserCtxHolder.getUserUid(),false
        ).ifPresentOrElse( post -> {
            // ensure the resource is indeed attached to the post, and belongs to current post.
            postResourceRepository.findByPostIdAndResourceUuidUuid(
                    postId, resourceUuid
            ).orElseThrow(ForbiddenException::new);
        }, () -> { // check the resource whether belong to a user
            resourceRepository.findByUuidAndStatus(resourceUuid, ResourceStatus.UPLOAD_PENDING)
                    .filter(res -> Objects.equals(res.getUploaderId(), UserCtxHolder.getUserUid()))
                    .orElseThrow(ForbiddenException::new);
        });
        // for single resource such like post coverage image, we shall check whether if there is audit task already exists
        // if exists, we replace the previous resource with new one. For content image, we suppose to be multiple resources,
        // and audit task - resource binding should be created in publish time, so here we simply bind resource to task
        // if task exists, otherwise do nothing and wait for publish to bind.
        resourceRepository.findByUuidAndStatus(resourceUuid, ResourceStatus.UPLOAD_PENDING).map(
                res-> {
                    cloudFileService.setAndValidResourceForCallback(
                            res,
                            CloudFSRoot.UPLOADS,
                            ResourceStatus.ORPHAN,// only bind when temporary saving or publishing.
                            ResourceType.IMAGE);
                    return resourceRepository.save(res);
                }
        ).orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid));
    }

    private <T> T buildPostDetailResp(
            Post post,
            java.util.function.Function<Post, T> mapper,
            DetailApplier<T> applier
    ) {
        T res = mapper.apply(post);
        List<OptionVO<Long>> tags = tagRepository.findTagOptionVOsByPostIdIn(List.of(post.getId()));
        OptionVO<Long> category = categoryRepository.findCategoryOptionVOByPostIdIn(List.of(post.getId())).getFirst();
        Resource coverageImgRes = post.getCoverageResource();
        CloudResPresignedUrlResp coverImg = null;
        if(coverageImgRes != null) {
            coverImg = cloudFileService.getReadUrlCached(
                    CloudFSRoot.UPLOADS,
                    coverageImgRes.getUuid(),
                    post.getId(),
                    TargetType.POST_COVERAGE_IMAGE
            );
        }
        applier.apply(res, tags, category, coverImg);
        return res;
    }

    @FunctionalInterface
    private interface DetailApplier<T> {
        void apply(
                T res,
                List<OptionVO<Long>> tags,
                OptionVO<Long> category,
                CloudResPresignedUrlResp coverImg
        );
    }

    private <T> Page<T> listCardPostsInternal(
            Specification<Post> spec,
            Pageable pageable,
            java.util.function.Function<Post, T> mapper,
            CardApplier<T> applier
    ) {
        Page<Long> postPageIds = postRepository.findAllIds(spec, pageable);
        List<Long> postIds = postPageIds.getContent();
        List<Post> posts = postRepository.findAllByIdInAndOrderBYCreatedAtDesc(postIds);
        // Post & Category Map
        Map<Long, List<OptionVO<Long>>> postTagMap = tagRepository.findTagDOByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(
                        IdOptionVOPackagedDO::getId,
                        Collectors.mapping(IdOptionVOPackagedDO::getOptionVo, Collectors.toList())
                ));

        Map<Long, OptionVO<Long>> postCategoryMap = categoryRepository.findCategoryDOByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        IdOptionVOPackagedDO::getId,
                        IdOptionVOPackagedDO::getOptionVo
                ));

        // Post coverage resource presigned url map
        Map<Long, String> postIdCoverImgKeyMap = posts.stream()
                .filter(post -> post.getCoverageResource() != null)
                .collect(Collectors.toMap(
                        Post::getId,
                        post -> post.getCoverageResource().getResourceKey()
                ));
        Map<Long, CloudResPresignedUrlResp> postCoverageImgMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                postIdCoverImgKeyMap,
                TargetType.POST_COVERAGE_IMAGE
        );
        // Post UserBrief Map
        List<Long> userUids = posts.stream()
                .map(p -> p.getAuthor().getUid())
                .distinct()
                .toList();
        Map<Long, UserBrief> postUserBriefMap = userBriefService.queryForMapUserIdBriefMap(userUids);
        return new PageImpl<>(
                posts.stream().map(post -> {
                    T res = mapper.apply(post);
                    applier.apply(res, post, postTagMap, postCategoryMap, postCoverageImgMap, postUserBriefMap);
                    return res;
                }).toList(),
                pageable,
                postPageIds.getTotalElements()
        );
    }

    @FunctionalInterface
    private interface CardApplier<T> {
        void apply(
                T res,
                Post post,
                Map<Long, List<OptionVO<Long>>> postTagMap,
                Map<Long, OptionVO<Long>> postCategoryMap,
                Map<Long, CloudResPresignedUrlResp> postCoverageImgMap,
                Map<Long, UserBrief> postUserBriefMap
        );
    }

    private record UploadItem(
            int originalIndex,
            String path,
            String uuidPlain,
            UUID uuid,
            FileExtension ext
    ) {}
}
