package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.UserBizType;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservice.api.request.PublicPostListReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservice.service.NotificationService;
import org.waterwood.waterfunservicecore.api.UploadItem;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservice.api.response.post.*;
import org.waterwood.waterfunservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunservice.service.post.TagService;
import org.waterwood.waterfunservicecore.api.moderation.PostAuditPayload;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.entity.post.PostResource;
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
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserFollowerRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserSettingRepository;
import org.waterwood.waterfunservicecore.entity.user.UserCounter;
import org.waterwood.waterfunservicecore.entity.user.UserFollowerId;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.spec.PostSpec;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservice.service.post.PostService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.infrastructure.validation.UploadValidator;
import org.waterwood.waterfunservicecore.services.audit.ContentAuditService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;
import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.services.audit.UserActivityLogService;
import org.waterwood.waterfunservicecore.services.search.PostSearchService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.IdGenerator;

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
    private final ContentAuditService contentAuditService;
    private final UserSettingRepository userSettingRepository;
    private final UserFollowerRepository userFollowerRepository;
    private final PostSearchService postSearchService;

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
        Post p = postRepository.findById(id)
                .orElseThrow(PostNotFoundException::new);
        if(!Objects.equals(p.getAuthor().getUid(), UserCtxHolder.getUserUid())){
            throw new BizException(BaseResponseCode.FORBIDDEN);
        }
        if(p.getCategory() != null){
            categoryRepository.decreaseUsageCountById(p.getCategory().getId(), 1);
        }
        List<Long> tagIds = p.getTags().stream().map(Tag::getId).toList();
        if(!tagIds.isEmpty()){
            tagRepository.decreaseUsageCountInIds(tagIds, 1);
        }
        postRepository.deleteById(id);
        userActivityLogService.record(UserCtxHolder.getUserUid(), UserActionType.DELETED, BusinessType.POST, id);
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
                    if (post.getAuthor() != null) {
                        res.setUserBrief(postUserBriefMap.get(post.getAuthor().getUid()));
                    }
                }
        );
    }

    @Override
    public Page<PostCardResp> listAnnouncements(Pageable pageable) {
        Specification<Post> spec = (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("publishedAt")));
            return cb.and(
                    cb.equal(root.get("type"), PostType.NOTICE),
                    cb.equal(root.get("status"), PostStatus.PUBLISHED),
                    cb.equal(root.get("isDeleted"), false)
            );
        };
        return listCardPosts(spec, pageable);
    }

    @Override
    public Page<PostCardResp> listHotPosts(Pageable pageable) {
        Page<Long> hotIds = postRepository.findHotPostIds(pageable);
        return listCardPostsInternal(hotIds, postMapper::toPostCardResponseDto, (res, post, postTagMap, postCategoryMap, postCoverageImgMap, postUserBriefMap) -> {
            Long postId = post.getId();
            res.setTags(postTagMap.getOrDefault(postId, Collections.emptyList()));
            res.setCategory(postCategoryMap.get(postId));
            res.setCoverImage(postCoverageImgMap.get(postId));
            if (post.getAuthor() != null) {
                res.setUserBrief(postUserBriefMap.get(post.getAuthor().getUid()));
            }
        });
    }

    @Override
    public Page<PostCardResp> listPublicCardPosts(PublicPostListReq req) {
        if (StringUtil.isNotBlank(req.getKeyword())) {
            Pageable searchPageable = PageRequest.of(
                    Math.max(req.getPage() - 1, 0), Math.min(req.getSize(), 20));
            Page<Long> searchIds = postSearchService.searchPostIds(req.getKeyword(), searchPageable);
            return listCardPostsInternal(searchIds, postMapper::toPostCardResponseDto, (res, post, postTagMap, postCategoryMap, postCoverageImgMap, postUserBriefMap) -> {
                Long postId = post.getId();
                res.setTags(postTagMap.getOrDefault(postId, Collections.emptyList()));
                res.setCategory(postCategoryMap.get(postId));
                res.setCoverImage(postCoverageImgMap.get(postId));
                if (post.getAuthor() != null) {
                    res.setUserBrief(postUserBriefMap.get(post.getAuthor().getUid()));
                }
            });
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "isPinned", "type")
                .and(Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt"));
        Pageable pageable = PageRequest
                .of(Math.max(req.getPage() - 1, 0), Math.min(req.getSize(), 20), sort);

        Specification<Post> spec = PostSpec.ofPublic(req.getCategoryId(), req.getTagIds(), null);
        Set<Long> excludedAuthorIds = getWorkVisibilityExcludedAuthorIds();
        if (!excludedAuthorIds.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.not(root.get("author").get("uid").in(excludedAuthorIds))
            );
        }
        return listCardPosts(spec, pageable);
    }

    private Set<Long> getWorkVisibilityExcludedAuthorIds() {
        Long viewerUid = UserCtxHolder.getUserUid();
        List<UserSetting> restrictedSettings = userSettingRepository.findByWorkVisibilityNot(ProfileVisibility.PUBLIC);
        if (restrictedSettings.isEmpty()) return Collections.emptySet();
        Set<Long> excluded = new HashSet<>();
        Set<Long> followerCheckIds = new HashSet<>();
        for (UserSetting us : restrictedSettings) {
            if (viewerUid != null && viewerUid.equals(us.getId())) continue;
            if (us.getWorkVisibility() == ProfileVisibility.PRIVATE) {
                excluded.add(us.getId());
            } else if (us.getWorkVisibility() == ProfileVisibility.FOLLOWERS) {
                followerCheckIds.add(us.getId());
            }
        }
        if (!followerCheckIds.isEmpty()) {
            Set<Long> followingIds;
            if (viewerUid == null) {
                followingIds = Collections.emptySet();
            } else {
                List<UserFollowerId> idsToCheck = followerCheckIds.stream()
                        .map(uid -> new UserFollowerId(uid, viewerUid))
                        .toList();
                followingIds = userFollowerRepository.findAllById(idsToCheck)
                        .stream()
                        .map(uf -> uf.getId().getUserUid())
                        .collect(Collectors.toSet());
            }
            for (Long uid : followerCheckIds) {
                if (!followingIds.contains(uid)) {
                    excluded.add(uid);
                }
            }
        }
        return excluded;
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

    @Transactional
    @Override
    public PostDetailResp getPostDetail(Long id) {
        Long currentUid = UserCtxHolder.getUserUid();

        Post post = postRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(PostNotFoundException::new);

        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getUid().equals(currentUid);

        // Non-author: must be publicly visible, and only count views for non-author
        if (!isAuthor) {
            if (post.getVisibility() != PostVisibility.PUBLIC
                    || post.getStatus() != PostStatus.PUBLISHED) {
                throw new PostNotFoundException();
            }
            // Check author's work visibility
            Long postAuthorUid = post.getAuthor().getUid();
            UserSetting authorSetting = userSettingRepository.findById(postAuthorUid).orElse(null);
            if (authorSetting != null) {
                if (authorSetting.getWorkVisibility() == ProfileVisibility.PRIVATE) {
                    throw new PostNotFoundException();
                }
                if (authorSetting.getWorkVisibility() == ProfileVisibility.FOLLOWERS
                        && (currentUid == null
                            || !userFollowerRepository.existsById(new UserFollowerId(postAuthorUid, currentUid)))) {
                    throw new PostNotFoundException();
                }
            }
            postRepository.increaseViewCount(id, 1);
        }

        PostDetailResp resp = postMapper.toPostDetailResp(post);
        List<OptionVO<Long>> tags = tagRepository.findTagOptionVOsByPostId(post.getId());

        CloudResPresignedUrlResp coverImg = null;
        Resource coverageImgRes = post.getCoverageResource();
        if (coverageImgRes != null) {
            coverImg = cloudFileService.getReadUrlCached(
                    CloudFSRoot.UPLOADS,
                    coverageImgRes.getResourceKey(),
                    post.getId(),
                    TargetType.POST_COVERAGE_IMAGE
            );
        }

        if (post.getCategory() != null) {
            resp.setCategory(post.getCategory().toOption());
        }
        resp.setTags(tags);
        resp.setCoverImage(coverImg);

        if (isAuthor) {
            resp.setVisibility(post.getVisibility());
            resp.setStatus(post.getStatus());
            resp.setEditStatus(post.getEditStatus());

            // Fallback: if main fields are empty (draft/never published), show edited content
            if (StringUtil.isBlank(resp.getTitle())) {
                resp.setTitle(post.getEditedTitle());
            }
            if (StringUtil.isBlank(resp.getSubtitle())) {
                resp.setSubtitle(post.getEditedSubtitle());
            }
            if (StringUtil.isBlank(resp.getContent())) {
                resp.setContent(post.getEditedContent());
            }
            if (StringUtil.isBlank(resp.getSummary())) {
                resp.setSummary(post.getEditedSummary());
            }
            if (resp.getCategory() == null && post.getEditedCategory() != null) {
                resp.setCategory(post.getEditedCategory().toOption());
            }
            boolean hasEditedTags = post.getEditedTagIds() != null && !post.getEditedTagIds().isEmpty();
            boolean showEditedContent = post.getEditStatus() == PostEditStatus.PENDING
                    || post.getEditStatus() == PostEditStatus.REJECTED;
            if (showEditedContent) {
                if (StringUtil.isNotBlank(post.getEditedTitle())) {
                    resp.setTitle(post.getEditedTitle());
                }
                if (StringUtil.isNotBlank(post.getEditedSubtitle())) {
                    resp.setSubtitle(post.getEditedSubtitle());
                }
                if (StringUtil.isNotBlank(post.getEditedContent())) {
                    resp.setContent(post.getEditedContent());
                }
                if (StringUtil.isNotBlank(post.getEditedSummary())) {
                    resp.setSummary(post.getEditedSummary());
                }
                if (post.getEditedCategory() != null) {
                    resp.setCategory(post.getEditedCategory().toOption());
                }
                if (hasEditedTags) {
                    resp.setTags(tagRepository.findTagOptionVosByIdsIn(post.getEditedTagIds()));
                }
            }
        }
        // Auto-generate summary from content if still blank
        resp.setSummary(StringUtil.fallbackSummary(resp.getSummary(), post.getContent(), 200));

        if (currentUid != null) {
            resp.setIsLiked(userLikeRepository.existsById(new UserLikeId(currentUid, post.getId())));
            resp.setIsCollected(userCollectRepository.existsById(new UserCollectId(currentUid, post.getId())));
        }
        if (post.getAuthor() != null) {
            resp.setUserBrief(userBriefService.getUserBrief(post.getAuthor().getUid()));
        }

        String content = resp.getContent();
        if (StringUtil.isNotBlank(content)) {
            Set<String> uuids = StringUtil.extraResPlaceholders(content);
            if (!uuids.isEmpty()) {
                List<PostResource> postResources = postResourceRepository
                        .findAllByPostIdAndResourceUuidUuidIn(post.getId(), uuids);
                Map<String, String> uuidToPath = postResources.isEmpty()
                        ? resourceRepository.findByUuidIn(uuids).stream()
                            .filter(r -> r.getStatus() != ResourceStatus.DELETED)
                            .collect(Collectors.toMap(Resource::getUuid, Resource::getResourceKey))
                        : postResources.stream()
                            .collect(Collectors.toMap(
                                    pr -> pr.getResourceUuid().getUuid(),
                                    pr -> pr.getResourceUuid().getResourceKey()));
                if (!uuidToPath.isEmpty()) {
                    Map<String, CloudResPresignedUrlResp> urlMap = cloudFileService.batchGetReadPublicUrlCached(
                            CloudFSRoot.UPLOADS, uuidToPath, TargetType.POST_CONTENT_IMAGE);
                    resp.setContent(StringUtil.replaceResPlaceholders(content,
                            urlMap.entrySet().stream()
                                    .filter(e -> e.getValue() != null && e.getValue().getUrl() != null)
                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()))));
                }
            }
        }

        return resp;
    }

    @Override
    public Long draftNew() {
        Post p = createNewDraftPost();
        postRepository.save(p);
        return p.getId();
    }

    @Transactional
    @Override
    public void publish(Long id, PostSaveReq req) {
        Post p = postRepository.findByIdAndAuthorUidAndIsDeleted(
                id, UserCtxHolder.getUserUid(), false
        ).orElseThrow(PostNotFoundException::new);
        publish(p, req);
    }

    @Transactional
    @Override
    public Long publishNewPost(PostSaveReq req) {
        Post p = createNewDraftPost();
        publish(p, req);
        return p.getId();
    }

    @Transactional
    @Override
    public Long saveNewPost(PostSaveReq req) {
        Post p = createNewDraftPost();
        save(p, req);
        return p.getId();
    }

    @Transactional
    @Override
    public void save(Long id, PostSaveReq request) {
        Post p = postRepository
                .findByIdAndAuthorUidAndIsDeleted(id, UserCtxHolder.getUserUid(),false)
                .orElseThrow(NotFoundException::new);
        save(p, request);
    }

    private Post createNewDraftPost(){
        Long id = IdGenerator.nextPostId();
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
        return postRepository.save(p);
    }

    /**
     * Public a post
     * @param p {@link Post} post entity, must contain id, not essentially exist in db
     * @param req {@link PostSaveReq}
     */
    private void publish(Post p, PostSaveReq req) {
        // save the resource and form and synchronize the resources.
        save(p, req);
        // Preparing for audit task
        AuditPayload payload = new PostAuditPayload(
                p.getEditedTitle(),
                p.getEditedSubtitle(),
                p.getEditedContent(),
                p.getEditedSummary(),
                p.getAuthor().getUid(),
                p.getEditedCoverImg(),
                p.getEditedCategory() != null ? p.getEditedCategory().getId() : null,
                p.getEditedTagIds(),
                p.getEditedNewTags(),
                null,
                null
        );
        if (p.getStatus() == PostStatus.PUBLISHED) {
            p.setEditStatus(PostEditStatus.PENDING);
        } else {
            p.setStatus(PostStatus.PENDING);
            siteStatisticRecorder.recordNewPost();
        }
        postRepository.save(p);
        // Collect all the resource include content image and coverage
        // usually save above, but we make sure add coverage image here
        Set<String> allUuids = new HashSet<>(StringUtil.extraResPlaceholders(p.getEditedContent()));
        if (StringUtil.isNotBlank(p.getEditedCoverImg())) {
            allUuids.add(p.getEditedCoverImg());
        }
        contentAuditService.handleUserSubmit(
                p.getId(),
                TargetType.POST,
                payload,
                new ArrayList<>(allUuids)
        );
    }

    /**
     * Save a post
     *
     * @param p   {@link Post} post entity, must contain id, not essentially exist in db
     * @param req {@link PostSaveReq}
     * @return id of targer post
     */
    private Long save(Post p, PostSaveReq req) {
        if (p.getAuthor() == null || !p.getAuthor().getUid().equals(UserCtxHolder.getUserUid())) {
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

        if (req.getCategoryId() != null) {
            Category c = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new CategoryReferenceInvalidException(req.getCategoryId()));
            p.setEditedCategory(c);
        } else {
            p.setEditedCategory(null);
        }
        // Below should be created when public, and must after audition, not in temporary saving.
        // List<Tag> newTagCreated = tagService.createNewTags(request.getNewTags(), UserCtxHolder.getUserUid());
        if(req.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(req.getTagIds());
            p.setEditedTagIds(tags.stream().map(Tag::getId).toList());
        }

        if (req.getNewTags() != null) {
            p.setEditedNewTags(req.getNewTags().stream()
                    .map(String::trim)
                    .filter(StringUtil::isNotBlank)
                    .toList());
        }
        postRepository.save(p);
        return p.getId();
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
        List<Resource> authorResources;
        if (p.getAuthor() != null) {
            authorResources = resourceRepository.findByUploaderIdAndUuidInAndStatusNot(
                    p.getAuthor().getUid(), unlinkedNewResUuids, ResourceStatus.DELETED
            );
        } else {
            authorResources = List.of();
        }

        savePostResource(p, authorResources);
        Set<String> allValidNewResUuids = new HashSet<>(linkedNewResUuids);
        allValidNewResUuids.addAll(authorResources.stream().map(Resource::getUuid).toList());
        if (originResUuids.equals(allValidNewResUuids)) {
            return;
        }
        Set<String> toOrphanUuids = new HashSet<>(originResUuids);
        toOrphanUuids.removeAll(allValidNewResUuids);
        Set<String> toActivateUuids = new HashSet<>(linkedNewResUuids);
        toActivateUuids.addAll(authorResources.stream().map(Resource::getUuid).toList());
        toActivateUuids.removeAll(originResUuids);

        log.info("Sync post resources for post {}, to activate: {}, to orphan: {}", p.getId(), toActivateUuids, toOrphanUuids);
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
    public String contentPreview(String content) {
        Set<String> resourceUuids = StringUtil.extraResPlaceholders(content);
        log.info(resourceUuids.toString());
        List<Resource> resources = resourceRepository.findByUploaderIdAndUuidInAndStatusNot(
                UserCtxHolder.getUserUid(), resourceUuids, ResourceStatus.DELETED
        );

        Map<String, String> resourceMap = resources.stream()
                .collect(Collectors.toMap(
                        Resource::getUuid,
                        Resource::getResourceKey
                ));

        Map<String, CloudResPresignedUrlResp> respMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS,
                resourceMap,
                TargetType.POST_CONTENT_IMAGE
        );
        log.info(respMap.toString());

        return StringUtil.replaceResPlaceholders(
                content,
                respMap.entrySet().stream()
                        .filter(e -> e.getValue() != null && e.getValue().getUrl() != null)
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> e.getValue().getUrl()
                        ))
        );
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
                resourceRepository.findByUuidAndStatusNot(p.getEditedCoverImg(), ResourceStatus.DELETED)
                        .map(res -> cloudFileService.getReadUrlCached(
                                CloudFSRoot.UPLOADS,
                                res.getResourceKey(),
                                p.getId(),
                                TargetType.POST_COVERAGE_IMAGE
                        ))
                        .orElse(null)
        );
        resp.setEditedCoverImg(p.getEditedCoverImg());

        if(p.getEditedCategory() != null) {
            resp.setEditedCategoryId(new OptionVO<>(p.getEditedCategory().getId(), null, p.getEditedCategory().getName(), false));
        }
        if( p.getEditedTagIds() != null) {
            List<OptionVO<Long>> editedTags = tagRepository.findTagOptionVosByIdsIn(p.getEditedTagIds());

            resp.setEditedTagIds(editedTags);
        }
        if(p.getEditedNewTags() != null) {
            resp.setEditedNewTagIds(p.getEditedNewTags());
        }

        return resp;
    }

    private void ensurePostPublished(Long postId) {
        Post post = postRepository.findByIdAndIsDeleted(postId, false)
                .orElseThrow(PostNotFoundException::new);
        // Allow interaction if: publicly visible AND has been published at least once (has publishedAt)
        if (post.getVisibility() != PostVisibility.PUBLIC || post.getPublishedAt() == null) {
            throw new PostNotFoundException();
        }
    }

    @Transactional
    @Override
    public void like(Long postId) {
        ensurePostPublished(postId);
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

    @Transactional
    @Override
    public void collection(Long postId) {
        ensurePostPublished(postId);
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

    @Override
    public MyPostsStatsResp getMyPostStats() {
        Long userUid = UserCtxHolder.getUserUid();
        long total = postRepository.countByAuthorUidAndIsDeleted(userUid, false);
        long published = postRepository.countByAuthorUidAndStatusAndIsDeleted(userUid, PostStatus.PUBLISHED, false);
        long draft = postRepository.countByAuthorUidAndStatusAndIsDeleted(userUid, PostStatus.DRAFT, false);
        long pending = postRepository.countByAuthorUidAndStatusAndIsDeleted(userUid, PostStatus.PENDING, false);
        long rejected = postRepository.countByAuthorUidAndStatusAndIsDeleted(userUid, PostStatus.REJECTED, false);
        long totalLikeCount = postRepository.sumLikeCountByAuthorUid(userUid);
        long followerCount = userCounterRepository.findByUserUid(userUid)
                .map(UserCounter::getFollowerCnt).orElse(0L);
        return MyPostsStatsResp.builder()
                .totalCount(total)
                .publishedCount(published)
                .draftCount(draft)
                .pendingCount(pending)
                .rejectedCount(rejected)
                .totalLikeCount(totalLikeCount)
                .followerCount(followerCount)
                .build();
    }

    @Transactional
    @Override
    public void batchDelete(List<Long> ids) {
        Long userUid = UserCtxHolder.getUserUid();
        List<Post> posts = postRepository.findAllByIdInAndAuthorUidAndIsDeleted(ids, userUid, false);
        if (posts.isEmpty()) return;
        for (Post p : posts) {
            if(p.getCategory() != null){
                categoryRepository.decreaseUsageCountById(p.getCategory().getId(), 1);
            }
            List<Long> tagIds = p.getTags().stream().map(Tag::getId).toList();
            if(!tagIds.isEmpty()){
                tagRepository.decreaseUsageCountInIds(tagIds, 1);
            }
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        postRepository.deleteByIdIn(postIds);
        for (Post p : posts) {
            userActivityLogService.record(userUid, UserActionType.DELETED, BusinessType.POST, p.getId());
        }
    }

    @Transactional
    @Override
    public void batchPublish(List<Long> ids) {
        Long userUid = UserCtxHolder.getUserUid();
        List<Post> posts = postRepository.findAllByIdInAndAuthorUidAndIsDeleted(ids, userUid, false);
        for (Post p : posts) {
            if (p.getStatus() != PostStatus.DRAFT) continue;
            PostSaveReq req = new PostSaveReq();
            req.setTitle(p.getEditedTitle() != null ? p.getEditedTitle() : p.getTitle());
            req.setSubtitle(p.getEditedSubtitle());
            req.setContent(p.getEditedContent() != null ? p.getEditedContent() : "");
            req.setSummary(p.getEditedSummary());
            req.setCoverageImgId(p.getEditedCoverImg());
            req.setCategoryId(p.getEditedCategory() != null ? p.getEditedCategory().getId() : null);
            req.setTagIds(p.getEditedTagIds() != null ? new HashSet<>(p.getEditedTagIds()) : new HashSet<>());
            req.setNewTags(p.getEditedNewTags() != null ? new HashSet<>(p.getEditedNewTags()) : new HashSet<>());
            publish(p, req);
        }
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
                        return cloudFileService.createAndSetUpUploadRes(item.uuidPlain(), item.path(), UserCtxHolder.getUserUid());
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



    private <T> Page<T> listCardPostsInternal(
            Specification<Post> spec,
            Pageable pageable,
            java.util.function.Function<Post, T> mapper,
            CardApplier<T> applier
    ) {
        Page<Long> postPageIds = postRepository.findAllIds(spec, pageable);
        return listCardPostsInternal(postPageIds, mapper, applier);
    }

    private <T> Page<T> listCardPostsInternal(
            Page<Long> postPageIds,
            java.util.function.Function<Post, T> mapper,
            CardApplier<T> applier
    ) {
        List<Long> postIds = postPageIds.getContent();
        List<Post> posts = postRepository.findAllByIdInAndOrderBYCreatedAtDesc(postIds);
        if (!postIds.isEmpty()) {
            Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
            posts = postIds.stream().map(postMap::get).filter(Objects::nonNull).toList();
        }
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
                .map(p -> p.getAuthor() != null ? p.getAuthor().getUid() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserBrief> postUserBriefMap = userBriefService.queryForMapUserIdBriefMap(userUids);
        return new PageImpl<>(
                posts.stream().map(post -> {
                    T res = mapper.apply(post);
                    applier.apply(res, post, postTagMap, postCategoryMap, postCoverageImgMap, postUserBriefMap);
                    return res;
                }).toList(),
                postPageIds.getPageable(),
                postPageIds.getTotalElements()
        );
    }
    @Override
    public void ensurePostReportable(Long postId) {
        Post post = postRepository.findByIdAndIsDeleted(postId, false)
                .orElseThrow(PostNotFoundException::new);
        if (post.isSystem()) {
            throw new ForbiddenException();
        }
    }

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
}
