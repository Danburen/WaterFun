package org.waterwood.waterfunservice.service.post.impl;

import jakarta.validation.constraints.NotBlank;
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
import org.waterwood.common.io.SimpleCloudObject;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservice.api.request.PutUserPostReq;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservice.api.response.post.PostAuthorCardResp;
import org.waterwood.waterfunservice.api.response.post.PostAuthorDetailResp;
import org.waterwood.waterfunservice.api.response.post.PostCardResp;
import org.waterwood.waterfunservice.api.response.post.PostDetailResp;
import org.waterwood.waterfunservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunservice.service.post.TagService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.post.*;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.resource.SourceType;
import org.waterwood.waterfunservicecore.exception.notfound.CategoryNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.PostNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.ResourceNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.CategoryReferenceInvalidException;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileType;
import org.waterwood.waterfunservicecore.utils.CosKeyPathGenerator;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.task.TargetType;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.exception.io.IllegalUploadCountException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservice.service.post.PostService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.utils.BizTargetIdPackager;
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

    @Override
    public void add(Post post, Set<Integer> tagIds) {
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

    @Override
    public void publish(Long id) {
        Post p = postRepository.findByIdAndAuthorUidAndIsDeletedAndStatus(
                id, UserCtxHolder.getUserUid(), false, PostStatus.DRAFT
        ).orElseThrow(PostNotFoundException::new);

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



        auditTaskRepository.save(task);
    }

    @Transactional
    @Override
    public void tempSave(Long id, PostSaveReq request) {
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
            p.setCoverImg(res.getResourceKey());
            res.setStatus(ResourceStatus.ACTIVE);
        }
        Category c = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryReferenceInvalidException(request.getCategoryId()));
        p.setEditedCategory(c);
        // Below should be create when public, not in temporary saving.
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
    public List<PresignedResp> handlePostCoverageImageUpload(UploadPolicyReq request) {
        Long bizId = Long.parseLong(request.getBizId());
        if(request.getExts().size() != 1){
            throw new IllegalUploadCountException(1);
        }

        FileExtension ext = FileExtension.fromExt(request.getExts().getFirst());
        if(TargetType.POST_COVERAGE_IMAGE.isAllowed(ext)){
            throw new UnsupportedOperationException("File type not allowed: " + ext.getExt());
        }

        postRepository.findByIdAndAuthorUidAndIsDeleted(
                bizId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(PostNotFoundException::new);


        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = BizTargetIdPackager.ofPost(bizId, BizType.POST_COVERAGE_IMAGE.name(), resourceUUID);
        String cosPath = CosKeyPathGenerator.of(resourceUUID, ext);

        Resource res = new Resource();
        res.setUuid(StringUtil.noDashUUIDString(resourceUUID));
        res.setResourceKey(cosPath);
        res.setResourceType(ResourceType.IMAGE);
        res.setUploaderId(UserCtxHolder.getUserUid());
        res.setSourceType(SourceType.USER_UPLOADED);
        resourceRepository.save(res);

        return List.of(cloudFileService.buildPutPolicyForUploads(cosPath, payload));
    }

    @Transactional
    @Override
    public List<PresignedResp> handlePostContentImageUpload(UploadPolicyReq request) {
        Long bizId = Long.parseLong(request.getBizId());
        postRepository.findByIdAndAuthorUidAndIsDeleted(
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
                        Resource res = new Resource();
                        res.setUuid(item.uuidPlain());
                        res.setResourceKey(item.path());
                        res.setResourceType(ResourceType.IMAGE);
                        res.setUploaderId(UserCtxHolder.getUserUid());
                        res.setSourceType(SourceType.USER_UPLOADED);
                        return res;
                    })
                    .toList();

            resourceRepository.saveAll(resources);

            List<BizUploadPayload> payloads = validItems.stream()
                    .map(item -> BizTargetIdPackager.ofPost(
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
        }
        return results;
    }

    @Transactional
    @Override
    public void handlePostImageUploadCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
        Assert.isTrue(payload.getType().equals(BizType.POST_COVERAGE_IMAGE.name())
                || payload.getType().equals(BizType.POST_CONTENT_IMAGE.name()),
                "Invalid biz type: " + payload.getType()
        );

        Long postId = Long.parseLong(payload.getBizId());
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                postId, UserCtxHolder.getUserUid(),false
        ).orElseThrow(() -> new NotFoundException("Post: " + postId));

        SimpleCloudObject obj = cloudFileService.detectAndAssertCloudFile(payload.getCosKey(), CloudFileType.IMAGE);

        TargetType type =  payload.getType().equals(BizType.POST_CONTENT_IMAGE.name()) ? TargetType.POST_CONTENT_IMAGE : TargetType.POST_COVERAGE_IMAGE;
        AuditTask task = auditTaskRepository
                .findByTargetIdAndTargetTypeAndStatus(payload.getBizId(), type, AuditStatus.PENDING)
                .orElseGet(() -> {
                    AuditTask newTask = new AuditTask();
                    newTask.setTargetId(payload.getBizId());
                    newTask.setSubmitAt(Instant.now());
                    newTask.setTargetType(type);
                    newTask.setSubmitter(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
                    return newTask;
                });
        auditTaskRepository.save(task);

        if(payload.getType().equals(BizType.POST_CONTENT_IMAGE.name())) {
            resourceRepository.findByUuidAndStatus(payload.getUploadId(), ResourceStatus.UPLOAD_PENDING).map(
                    res-> {
                        res.setSizeBytes(obj.getFileMeta().getSize());
                        res.setMimeType(obj.getFileMeta().getMimeType());
                        res.setSourceType(SourceType.USER_UPLOADED);
                        res.setStatus(ResourceStatus.ORPHAN);
                        return resourceRepository.save(res);
                    }
            ).orElseThrow(() -> new NotFoundException("Resource with uuid: " + payload.getUploadId()));
        } else {
            AuditResource auditRes = auditTaskResourceRepository
                    .findByTaskId(task.getId())
                    .orElseGet(() -> {
                        AuditResource ar = new AuditResource();
                        ar.setTask(task);
                        return auditTaskResourceRepository.save(ar);
                    });

            Resource res = auditRes.getResource();
            if(res == null) throw new NotFoundException("Audit resource not found: " + auditRes.getId());
            if ( res.getResourceKey() != null) {
                final String oldKey = res.getResourceKey();
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                try {
                                    cloudFileService.removeFile(CloudFSRoot.UPLOADS, oldKey);
                                } catch (Exception e) {
                                    log.error("Failed to remove old cloud file: {}", oldKey, e);
                                }
                            }
                        }
                );
            }

            res.setResourceKey(obj.getKey());
            res.setResourceType(ResourceType.IMAGE);
            res.setSizeBytes(obj.getFileMeta().getSize());
            res.setMimeType(obj.getFileMeta().getMimeType());
            res.setSourceType(SourceType.USER_UPLOADED);
            res.setUploaderId(UserCtxHolder.getUserUid());
            res.setStatus(ResourceStatus.ACTIVE);
            resourceRepository.save(res);
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
        CloudResPresignedUrlResp coverImg = cloudFileService.getReadUrlCached(
                CloudFSRoot.UPLOADS,
                post.getCoverImg(),
                post.getId(),
                TargetType.POST_COVERAGE_IMAGE
        );
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

        Map<Long, CloudResPresignedUrlResp> postCoverageImgMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                posts.stream()
                        .map(Post::getCoverImg)
                        .toList(),
                postIds,
                TargetType.POST_COVERAGE_IMAGE);

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
