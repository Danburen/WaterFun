package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.UploadContext;
import org.waterwood.waterfunservice.api.request.PutUserPostReq;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
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
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.resource.SourceType;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.notfound.PostNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.AuditTaskReferenceInvalidException;
import org.waterwood.waterfunservicecore.exception.reference.CategoryReferenceInvalidException;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.*;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.utils.CosKeyPathGenerator;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.exception.io.IllegalUploadCountException;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservice.service.post.PostService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.utils.ContentIdGenerator;

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

    @Override
    public void add(Post post, Set<Long> tagIds) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        User u = userCoreService.getUserByUid(UserCtxHolder.getUserUid());
        post.setAuthor(u);
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
        if(p.getAuthor() == userCoreService.getUserByUid(UserCtxHolder.getUserUid())){
            postRepository.deleteById(id);
        }else{
            throw new BizException(BaseResponseCode.FORBIDDEN);
        }
    }

    @Override
    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(()-> new BizException(BaseResponseCode.NOT_FOUND));
    }

    @Override
    public Page<PostCardResp> listCardPosts(Specification<Post> spec, Pageable pageable) {
        return listCardPostsInternal(
                spec,
                pageable,
                postMapper::toPostCardResponseDto,
                (res, post, postTagMap, postCategoryMap, postCoverageImgMap) -> {
                    res.setTags(postTagMap.getOrDefault(post.getId(), Collections.emptyList()));
                    res.setCategory(postCategoryMap.get(post.getId()));
                    res.setCoverImage(postCoverageImgMap.get(post.getId()));
                }
        );
    }

    @Override
    public Page<PostAuthorCardResp> listAuthorCardPosts(Specification<Post> spec, Pageable pageable) {
        return listCardPostsInternal(
                spec,
                pageable,
                postMapper::toPostAuthorCardResp,
                (res, post, postTagMap, postCategoryMap, postCoverageImgMap) -> {
                    res.setTags(postTagMap.getOrDefault(post.getId(), Collections.emptyList()));
                    res.setCategory(postCategoryMap.get(post.getId()));
                    res.setCoverImage(postCoverageImgMap.get(post.getId()));
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
    public void updatePost(Long id, PutUserPostReq req) {
        Post p = postRepository.findByIdAndAuthorUidAndIsDeletedAndStatus(
                id, UserCtxHolder.getUserUid(), false, PostStatus.DRAFT
        ).orElseThrow(() -> new BizException(BaseResponseCode.NOT_FOUND));
        postMapper.partialUpdate(req, p);
        if (req.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(req.getTagIds());
            p.setTags(tags);
        }
        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId())
                    .ifPresent(p::setCategory);
        }
        postRepository.save(p);
    }

    @Transactional
    @Override
    public void publish(Long id, PostSaveReq req) {
        Post p = postRepository.findByIdAndAuthorUidAndIsDeletedAndStatus(
                id, UserCtxHolder.getUserUid(), false, PostStatus.DRAFT
        ).orElseThrow(PostNotFoundException::new);
        this.save(id, req); // save edited content to temp fields, and sync resources status
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
        String editedContent =  p.getEditedContent();
        task.setPayload(new PostAuditPayload(
                p.getEditedTitle(),
                p.getEditedSubtitle(),
                editedContent,
                p.getEditedSummary(),
                p.getCoverageResourceUuid() != null ? p.getCoverageResourceUuid().getUuid() : null,
                p.getEditedCategory() != null ? p.getEditedCategory().getId() : null,
                p.getEditedTagIds() != null ? p.getEditedTagIds() : null,
                p.getEditedNewTags() != null ? p.getEditedNewTags() : null
        ).toJson());

        Set<String> resourceUuids = StringUtil.extraResPlaceholders(editedContent);
        List<Resource> resources = resourceRepository.findByUuidInAndStatus(resourceUuids, ResourceStatus.ACTIVE);
        List<AuditResource> auditResources = resources.stream().map(res -> { // bind audit - resource
            AuditResource ar = new AuditResource();
            ar.setTask(task);
            ar.setResource(res);
            return ar;
        }).toList();

        auditTaskResourceRepository.saveAll(auditResources);
        auditTaskRepository.save(task);
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

        syncResources(p.getEditedContent(), request.getContent());
        p.setEditedContent(request.getContent());
        p.setEditedSummary(request.getSummary());
        if(request.getCoverageImgId() != null) {
            Resource res = resourceRepository.findByUuidAndStatus(request.getCoverageImgId(), ResourceStatus.ORPHAN)
                    .orElseThrow(() -> new ResourceReferenceInvalidException(request.getCoverageImgId()));
            p.setCoverageResourceUuid(res);
            res.setStatus(ResourceStatus.ACTIVE);
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
        p.setEditedNewTags(request.getNewTags().stream().toList());
        postRepository.save(p);
    }

    /**
     * Synchronize resource for given content
     * must run in transactional
     * @param originContent original content
     * @param newContent new content
     */
    private void syncResources(String originContent, String newContent) {
        // original resources should all be ACTIVE(attached) instead of ORPHAN
        Set<String> originResUuids = StringUtil.extraResPlaceholders(originContent);
        Set<String> newResUuids = StringUtil.extraResPlaceholders(newContent);
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
    public List<PresignedResp> handlePostCoverageImageUpload(UploadPolicyReq request) {
        Long bizId = Long.parseLong(request.getBizId());
        if(request.getExts().size() != 1){
            throw new IllegalUploadCountException(1);
        }

        FileExtension ext = FileExtension.fromExt(request.getExts().getFirst());
        if(! TargetType.POST_COVERAGE_IMAGE.isAllowed(ext)){
            throw new UnsupportedOperationException("File type not allowed: " + ext.getExt());
        }

        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                bizId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(PostNotFoundException::new);


        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = UploadContext.<Long>builder()
                .bizId(bizId)
                .bizType(request.getBizType())
                .resourceUuid(resourceUUID.toString().replace("-", ""))
                .build()
                .toPayload();
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
    public List<PresignedResp> handlePostContentImageUpload(UploadPolicyReq request) {
        Long bizId = Long.parseLong(request.getBizId());
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                bizId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(() -> new NotFoundException("Post: " + bizId));

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
                        return cloudFileService.CreateAndSetUpUploadRes(item.uuidPlain, item.path, UserCtxHolder.getUserUid());
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
                            BizType.POST_CONTENT_IMAGE.name(),
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
    public void handlePostImageUploadCallback(CloudPutCallbackReq request, UploadContext<Long> ctx) {
        Assert.isTrue(ctx.getBizType() == BizType.POST_COVERAGE_IMAGE
                        || ctx.getBizType() == BizType.POST_CONTENT_IMAGE,
                "Invalid biz type: " + ctx.getBizType()
        );

        Long postId = ctx.getBizId();
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                postId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(() -> new NotFoundException("Post: " + postId));

        TargetType type =  ctx.getBizType() == BizType.POST_CONTENT_IMAGE ? TargetType.POST_CONTENT_IMAGE : TargetType.POST_COVERAGE_IMAGE;

        String resourceUuid = ctx.getResourceUuid();
        postResourceRepository.findByPostIdAndResourceUuidUuid(
                postId, resourceUuid
        ).orElseThrow(ForbiddenException::new); // ensure the resource is indeed attached to the post, and belongs to current post.

        if(ctx.getBizType() == BizType.POST_CONTENT_IMAGE) {
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
        } else if(ctx.getBizType() == BizType.POST_COVERAGE_IMAGE){
            AuditTask task = auditTaskRepository.findByTargetIdAndTargetTypeAndStatus(String.valueOf(ctx.getBizId()), type, AuditStatus.PENDING)
                    .orElse(null);
            Resource res;
            if(task != null){ // task is already exists, simply update resource and register old resource deletion after commit if exists
                AuditResource auditRes = auditTaskResourceRepository
                        .findByTaskId(task.getId())
                        .orElseGet(() -> {
                            AuditResource ar = new AuditResource();
                            ar.setTask(task);
                            ar.setResource(resourceRepository.findByUuidAndStatus(resourceUuid, ResourceStatus.UPLOAD_PENDING)
                                    .orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid))
                            );
                            return auditTaskResourceRepository.save(ar);
                        });
                res = auditRes.getResource();
                if(res == null) throw new ResourceReferenceInvalidException(resourceUuid);
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
                        ResourceStatus.ACTIVE,
                        ResourceType.IMAGE);
                resourceRepository.save(res);
            } else {
                // Audit task supposes to be created before callback, at the phase of
                // preparing uploading(policy requesting)
                throw new AuditTaskReferenceInvalidException(String.valueOf(ctx.getBizId()));
            }
        }
    }

    private <T> T buildPostDetailResp(
            Post post,
            java.util.function.Function<Post, T> mapper,
            DetailApplier<T> applier
    ) {
        T res = mapper.apply(post);
        List<OptionVO<Integer>> tags = tagRepository.findTagsByPostIds(List.of(post.getId())).stream()
                .map(arr -> (OptionVO<Integer>) arr[1])
                .toList();
        OptionVO<Integer> category = categoryRepository.findCategoryByPostIds(List.of(post.getId())).stream()
                .map(arr -> (OptionVO<Integer>) arr[1])
                .findFirst()
                .orElse(null);
        Resource coverageImgRes = post.getCoverageResourceUuid();
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
        List<Post> posts = postRepository.findAllById(postIds);

        Map<Long, List<OptionVO<Integer>>> postTagMap = tagRepository.findTagsByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(
                        arr -> (long) arr[0],
                        Collectors.mapping(arr -> (OptionVO<Integer>) arr[1], Collectors.toList())
                ));

        Map<Long, OptionVO<Integer>> postCategoryMap = categoryRepository.findCategoryByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        arr -> (long) arr[0],
                        arr -> (OptionVO<Integer>) arr[1],
                        (a, b) -> a
                ));

        Map<Long, String> postIdCoverImgKeyMap = new HashMap<>();
        for(int i = 0; i < postIds.size(); i++){
            postIdCoverImgKeyMap.put(postIds.get(i), posts.get(i).getCoverageResourceUuid().getResourceKey());
        }

        Map<Long, CloudResPresignedUrlResp> postCoverageImgMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                postIdCoverImgKeyMap,
                TargetType.POST_COVERAGE_IMAGE
        );

        return new PageImpl<>(
                posts.stream().map(post -> {
                    T res = mapper.apply(post);
                    applier.apply(res, post, postTagMap, postCategoryMap, postCoverageImgMap);
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
                Map<Long, CloudResPresignedUrlResp> postCoverageImgMap
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
