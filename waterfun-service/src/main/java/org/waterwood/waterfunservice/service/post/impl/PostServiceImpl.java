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
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;
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

    @Value("${user.quota.collect:10000}")
    private Long userCollectExceedLimit;

    @Override
    public void add(Post post, Set<Long> tagIds) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        post.setAuthor(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        post.setSlug(identifierGenerator.generateSlug(post.getTitle(), postRepository));
        post.setTags(tags);
        postRepository.save(post);
    }

    @Override
    public Page<Post> listPosts(Specification<Post> spec, Pageable pageable) {
        return postRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post p = postRepository.getReferenceById(id);
        if(p.getAuthor() == userRepository.getReferenceById(UserCtxHolder.getUserUid())) {
            postRepository.deleteById(id);
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
        return id;
    }

    @Transactional
    @Override
    public void publish(Long id, PostSaveReq req) {
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                id, UserCtxHolder.getUserUid(), false
        ).orElseThrow(PostNotFoundException::new);
        this.save(id, req); // saved edited content to temp fields, and sync resources status
        String targetId = id.toString();
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
        task.setContentFormat(AuditContentFormat.MARKDOWN); // TODO supposed need to determines content format
        task.setContent(p.getEditedContent());
        String editedContent =  p.getEditedContent();
        task.setPayload(new PostAuditPayload(
                p.getEditedTitle(),
                p.getEditedSubtitle(),
                editedContent,
                p.getEditedSummary(),
                p.getEditedCoverImg() != null ? p.getEditedCoverImg() : null,
                p.getEditedCategory() != null ? p.getEditedCategory().getId() : null,
                p.getEditedTagIds() != null ? p.getEditedTagIds() : null,
                p.getEditedNewTags() != null ? p.getEditedNewTags() : null,
                null // store is not needed
        ).toJson());
        p.setStatus(PostStatus.PENDING);

        postRepository.save(p);
        task = auditTaskRepository.save(task);

        Set<String> resourceUuids = StringUtil.extraResPlaceholders(editedContent);
        List<String> linkedResourceUuids = getUnDeletedLinkedResourceUuids(p.getId());
        // all resource suppose to stay in active status after temp save
        // content image resource
        AuditTask finalTask = task;
        List<AuditResource> auditResources = new ArrayList<>(resourceRepository
                .findByUuidInAndStatus(resourceUuids, ResourceStatus.ACTIVE)
                .stream().filter(r -> linkedResourceUuids.contains(r.getUuid()))
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
                .toList());
        // coverage image resource
        String reqCoverageUuid = req.getCoverageImgId();
        Resource postCoverageImageRes = resourceRepository
                .findByUuidAndStatus(reqCoverageUuid, ResourceStatus.ACTIVE)
                .filter(r -> linkedResourceUuids.contains(r.getUuid()))
                .orElseThrow(() -> new ResourceReferenceInvalidException(reqCoverageUuid));
        AuditResource ar = new AuditResource();
        AuditResourceId arId = new AuditResourceId();
        arId.setResourceUuid(postCoverageImageRes.getUuid());
        arId.setTaskId(task.getId());

        ar.setId(arId);
        ar.setTask(task);
        ar.setResource(postCoverageImageRes);
        auditResources.add(ar);
        auditTaskResourceRepository.saveAll(auditResources);
    }

    @Transactional
    @Override
    public void save(Long id, PostSaveReq request) {
        Post p = postRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(NotFoundException::new);

        if (!p.getAuthor().getUid().equals(UserCtxHolder.getUserUid())) {
            throw new NotFoundException();
        }

        p.setEditedTitle(request.getTitle());
        p.setEditedSubtitle(request.getSubtitle());

        syncResource(p.getId(), p.getEditedContent(), request.getContent());
        p.setEditedContent(request.getContent());
        p.setEditedSummary(request.getSummary());

        resourceRepository.findByUuidAndStatusNot( // unbind old coverage
                p.getEditedCoverImg(), ResourceStatus.DELETED
        ).ifPresent(res -> {
            res.setStatus(ResourceStatus.ORPHAN);
            resourceRepository.save(res);
        });

        if(request.getCoverageImgId() != null) {
            Resource res = resourceRepository.findByUuidAndStatusNot(request.getCoverageImgId(), ResourceStatus.DELETED)
                    .orElseThrow(() -> new ResourceReferenceInvalidException(request.getCoverageImgId()));
            res.setStatus(ResourceStatus.ACTIVE);
            p.setEditedCoverImg(res.getUuid());
        }else {
            p.setEditedContent(null);
        }
        Category c = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryReferenceInvalidException(request.getCategoryId()));
        p.setEditedCategory(c);
        // Below should be created when public, and must after audition, not in temporary saving.
        // List<Tag> newTagCreated = tagService.createNewTags(request.getNewTags(), UserCtxHolder.getUserUid());
        if(request.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            p.setEditedTagIds(tags.stream().map(Tag::getId).toList());
        }
        p.setEditedNewTags(request.getNewTags().stream()
                .map(str -> str.trim().replace("[^a-z0-9\\\\-]", ""))
                .toList());
        postRepository.save(p);
    }

    /**
     * Synchronize resource for given content for a post
     * only those resource in the new content linked by {@link PostResource} will be synchronized.
     * must run in transactional
     *
     * @param postId        target post id to identify whether new content resource belongs to the post.
     * @param originContent original content
     * @param newContent    new content
     */
    private void syncResource(Long postId, String originContent, String newContent) {
        // original resources should all be ACTIVE(attached) instead of ORPHAN
        Set<String> originResUuids = StringUtil.extraResPlaceholders(originContent);;
        // we shall list all available post resources and filter those resource not belong to the post, since upload
        // phase would link resource to post. those resource which status stay ORPHAN, would be clean in schedule tasks.
        List<String> postAvailableResources = getUnDeletedLinkedResourceUuids(postId);
        Set<String> newResUuids = StringUtil.extraResPlaceholders(newContent).stream().filter(
                postAvailableResources::contains
        ).collect(Collectors.toSet());
        if (originResUuids.equals(newResUuids)) {
            return;
        }
        Set<String> toOrphanUuids = new HashSet<>(originResUuids);
        toOrphanUuids.removeAll(newResUuids);
        Set<String> toActivateUuids = new HashSet<>(newResUuids);
        toActivateUuids.removeAll(originResUuids);
        if (!toActivateUuids.isEmpty()) {
            resourceRepository.batchUpdateStatus(ResourceStatus.ACTIVE, toActivateUuids);
        }
        if (!toOrphanUuids.isEmpty()) {
            resourceRepository.batchUpdateStatus(ResourceStatus.ORPHAN, toOrphanUuids);
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
        if(p.getTags() != null) {
            resp.setEditedTagIds(p.getTags().stream()
                    .map(tag -> OptionVO.<Long>builder()
                            .id(tag.getId())
                            .name(tag.getName())
                            .build())
                    .toList());
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
                        },
                        () -> {
                            UserLike newLike = new UserLike();
                            newLike.setId(new UserLikeId(userUid, postId));
                            userLikeRepository.save(newLike);
                            userCounterRepository.increaseUserLikeCount(userUid, 1);
                            postRepository.increaseLikeCount(postId, 1);
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
                            userCounterRepository.decreaseUserLikeCount(userUid, 1);
                            postRepository.decreaseCollectCount(postId, 1);
                            userCollectRepository.delete(collection);
                        },
                        () -> {
                            if (uc.getCollectCnt() >= userCollectExceedLimit) {
                                throw new UserCollectExceedLimitException();
                            }
                            UserCollect newCollection = new UserCollect();
                            newCollection.setId(new UserCollectId(userUid, postId));
                            userCollectRepository.save(newCollection);
                            userCounterRepository.increaseUserLikeCount(userUid, 1);
                            postRepository.increaseCollectCount(postId, 1);
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
        Long bizId = Long.parseLong(request.getBizId());
        FileExtension ext =  UploadValidator.validateSingleFileUpload(request, TargetType.POST_COVERAGE_IMAGE);
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                bizId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(PostNotFoundException::new);

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

        PostResource postResource = new PostResource();
        PostResourceId postResourceId = new PostResourceId();
        postResourceId.setResourceUuid(res.getUuid());
        postResourceId.setPostId(p.getId());

        postResource.setId(postResourceId);
        postResource.setResourceUuid(res);
        postResource.setPost(p);

        resourceRepository.save(res);
        postResourceRepository.save(postResource);

        return List.of(cloudFileService.buildPutPolicyForUploads(cosPath, payload));
    }

    @Transactional
    @Override
    public List<PresignedResp> handlePostContentImageUpload(UserUploadPolicyReq request) {
        Long bizId = Long.parseLong(request.getBizId());
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                bizId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(PostNotFoundException::new);

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
            List<PostResource> postResources = resources.stream()
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

            resourceRepository.saveAll(resources);
            postResourceRepository.saveAll(postResources);
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
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                postId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(() -> new NotFoundException("Post: " + postId));

        String resourceUuid = ctx.getResourceUuid();
        postResourceRepository.findByPostIdAndResourceUuidUuid(
                postId, resourceUuid
        ).orElseThrow(ForbiddenException::new); // ensure the resource is indeed attached to the post, and belongs to current post.
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

        if(ctx.getBizType() == UserBizType.POST_COVERAGE_IMAGE){
            AuditTask task = auditTaskRepository
                    .findByTargetIdAndTargetTypeAndStatus(
                            String.valueOf(ctx.getBizId()), TargetType.POST, AuditStatus.PENDING)
                    .orElse(null);
            Resource res;
            if(task != null) { // task is already exists, simply update resource and register old resource deletion after commit if exists
                AuditResource auditRes = auditTaskResourceRepository
                        .findByTaskId(task.getId())
                        .orElseGet(() -> {
                            AuditResource ar = new AuditResource();
                            AuditResourceId arId = new AuditResourceId();
                            arId.setTaskId(task.getId());
                            arId.setResourceUuid(resourceUuid);
                            ar.setId(arId);
                            ar.setTask(task);
                            ar.setResource(resourceRepository.findByUuidAndStatus(resourceUuid, ResourceStatus.UPLOAD_PENDING)
                                    .orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid))
                            );
                            return auditTaskResourceRepository.save(ar);
                        });
                res = auditRes.getResource();
                if (res == null) throw new ResourceReferenceInvalidException(resourceUuid);
                if (res.getResourceKey() != null) {
                    final String oldKey = res.getResourceKey();
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    // todo supposed to be after completion of whole transaction including audit task
                                    // creation, but currently no such callback, need to ensure no exception throw after this point
                                    // to avoid the case that old resource is removed but new resource is not set successfully.
                                    try {
                                        cloudFileService.removeFile(CloudFSRoot.UPLOADS, oldKey);
                                    } catch (Exception e) {
                                        log.error("Failed to remove old cloud file: {}", oldKey, e);
                                    }
                                }
                            }
                    );
                }
                cloudFileService.setAndValidResourceForCallback(
                        res,
                        CloudFSRoot.UPLOADS,
                        // the result of setting this to active instead of orphan is that task is not null,
                        // this only happened when a task is established before callback, which means user upload
                        // coverage already upload before(or null) the new one(this) would replace it(bind at the same time)
                        ResourceStatus.ACTIVE,
                        ResourceType.IMAGE);
                resourceRepository.save(res);
            }
            else {
                // Audit task supposes to be created before callback, at the phase of
                // preparing uploading(policy requesting)
            }
        }
    }

    private <T> T buildPostDetailResp(
            Post post,
            java.util.function.Function<Post, T> mapper,
            DetailApplier<T> applier
    ) {
        T res = mapper.apply(post);
        List<OptionVO<Integer>> tags = tagRepository.findTagsByPostIdIn(List.of(post.getId())).stream()
                .map(arr -> (OptionVO<Integer>) arr[1])
                .toList();
        OptionVO<Integer> category = categoryRepository.findCategoryByPostIdIn(List.of(post.getId())).stream()
                .map(arr -> (OptionVO<Integer>) arr[1])
                .findFirst()
                .orElse(null);
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
                List<OptionVO<Integer>> tags,
                OptionVO<Integer> category,
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
        Map<Long, List<OptionVO<Integer>>> postTagMap = tagRepository.findTagsByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(
                        arr -> (long) arr[0],
                        Collectors.mapping(arr -> (OptionVO<Integer>) arr[1], Collectors.toList())
                ));

        Map<Long, OptionVO<Integer>> postCategoryMap = categoryRepository.findCategoryByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        arr -> (long) arr[0],
                        arr -> (OptionVO<Integer>) arr[1],
                        (a, b) -> a
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
                Map<Long, List<OptionVO<Integer>>> postTagMap,
                Map<Long, OptionVO<Integer>> postCategoryMap,
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
