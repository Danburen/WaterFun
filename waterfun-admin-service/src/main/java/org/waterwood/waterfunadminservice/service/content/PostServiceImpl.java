package org.waterwood.waterfunadminservice.service.content;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunadminservice.api.request.DeletePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.AssignTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.CreatePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutPostReq;
import org.waterwood.waterfunadminservice.api.response.content.PostResponse;
import org.waterwood.waterfunadminservice.api.response.content.audit.PostBrief;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostTag;
import org.waterwood.waterfunservicecore.entity.post.PostTagId;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.PostNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.CategoryReferenceInvalidException;
import org.waterwood.waterfunservicecore.exception.reference.PostReferenceInvalidException;
import org.waterwood.waterfunservicecore.exception.reference.UserReferenceInvalidException;
import org.waterwood.waterfunservicecore.entity.post.PostResource;
import org.waterwood.waterfunservicecore.entity.post.PostResourceId;
import org.waterwood.waterfunservicecore.infrastructure.persistence.*;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.infrastructure.utils.IdGenerator;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final IdentifierGenerator identifierGenerator;
    private final UserRepository userRepository;
    private final PostTagRepository postTagRepository;
    private final ResourceRepository resourceRepository;
    private final PostResourceRepository postResourceRepository;
    private final UserBriefService userBriefService;
    private final CloudFileService cloudFileService;

    @Override
    public Page<Post> listPosts(Specification<Post> spec, Pageable pageable) {
        return postRepository.findAll(spec, pageable);
    }

    @Override
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Post ID: " + id)
        );
    }

    public PostResponse getPostDetailResponse(Long id) {
        Post p = getPostById(id);
        PostResponse resp = postMapper.toPostResponseDto(p);

        // Cover image — prefer editedCoverImg (pending edit), else current coverageResource
        String coverUuid = p.getEditedCoverImg();
        if (StringUtil.isNotBlank(coverUuid)) {
            resp.setCoverImg(coverUuid);
            resourceRepository.findByUuidAndStatusNot(coverUuid, ResourceStatus.DELETED)
                    .ifPresent(res -> resp.setCoverImage(cloudFileService.getReadUrlCached(
                            CloudFSRoot.UPLOADS, res.getResourceKey(), p.getId(), TargetType.POST_COVERAGE_IMAGE
                    )));
        } else if (p.getCoverageResource() != null) {
            resp.setCoverImg(p.getCoverageResource().getUuid());
            resp.setCoverImage(cloudFileService.getReadUrlCached(
                    CloudFSRoot.UPLOADS, p.getCoverageResource().getResourceKey(), p.getId(), TargetType.POST_COVERAGE_IMAGE
            ));
        }

        // Resolve res://<uuid> in content
        resolveContentImages(p, resp, p.getContent(), resp::setContentHtml);

        // Edited fields
        resp.setEditedTitle(p.getEditedTitle());
        resp.setEditedSubtitle(p.getEditedSubtitle());
        resp.setEditedContent(p.getEditedContent());
        resp.setEditedSummary(p.getEditedSummary());
        resp.setEditedCoverImg(coverUuid);
        resp.setEditedCategoryId(p.getEditedCategory() != null ? p.getEditedCategory().getId() : null);
        resp.setEditedTagIds(p.getEditedTagIds());

        // Resolve res://<uuid> in edited content
        resolveContentImages(p, resp, p.getEditedContent(), resp::setEditedContentHtml);

        return resp;
    }

    private void resolveContentImages(Post p, PostResponse resp, String content, Consumer<String> setter) {
        if (StringUtil.isBlank(content)) return;
        Set<String> uuids = StringUtil.extraResPlaceholders(content);
        if (uuids.isEmpty()) return;
        Map<String, String> uuidToKey = resourceRepository.findByUuidIn(uuids).stream()
                .filter(r -> r.getStatus() != ResourceStatus.DELETED)
                .collect(Collectors.toMap(Resource::getUuid, Resource::getResourceKey));
        if (uuidToKey.isEmpty()) return;
        Map<String, CloudResPresignedUrlResp> urlMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.UPLOADS, uuidToKey, TargetType.POST_CONTENT_IMAGE);
        setter.accept(StringUtil.replaceResPlaceholders(content,
                urlMap.entrySet().stream()
                        .filter(e -> e.getValue() != null && e.getValue().getUrl() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()))));
    }

    @Override
    @Transactional
    public void deletePostById(Long id) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post ID: " + id));
        if(p.getCategory() != null){
            categoryRepository.decreaseUsageCountById(p.getCategory().getId(), 1);
        }
        List<Long> tagIds = p.getTags().stream().map(Tag::getId).toList();
        if(!tagIds.isEmpty()){
            tagRepository.decreaseUsageCountInIds(tagIds, 1);
        }
        postTagRepository.deleteByPostId(id);
        postRepository.deleteByIdIn(List.of(id));
    }

    @Transactional
    @Override
    public void update(Long id, PutPostReq req) {
        Post p = postRepository.findById(id).orElseThrow(
                PostNotFoundException::new
        );
        String oldContent = p.getContent();
        postMapper.partialUpdate(req, p);
        String newContent = p.getContent(); // content after partial update (may be same as old if req.content is null)
        if(req.getAuthorId() != null) {
            User u = userRepository.findById(req.getAuthorId()).orElseThrow(
                    () -> new UserReferenceInvalidException(req.getAuthorId())
            );
            p.setAuthor(u);
        }
        Resource oldRes = p.getCoverageResource();
        if(req.getCoverageUuid() == null) {
            if(oldRes != null) {
                oldRes.setStatus(ResourceStatus.ORPHAN);
            }
            p.setCoverageResource(null);
            p.setEditedCoverImg(null);
        } else {
            if (oldRes != null) {
                oldRes.setStatus(ResourceStatus.ORPHAN);
            }
            Resource res = resourceRepository.findByUuidAndStatus(req.getCoverageUuid(), ResourceStatus.ORPHAN)
                    .orElseThrow(() -> new NotFoundException(req.getCoverageUuid()));
            res.setStatus(ResourceStatus.ACTIVE);
            p.setCoverageResource(res);
            p.setEditedCoverImg(null);
        }

        syncContentResources(p, newContent, oldContent);

        if(req.getTagIds() != null) {
            List<Long> oldTagIds = postTagRepository.findTagIdsByPostId(id);
            Set<Long> newTagIdSet = new HashSet<>(req.getTagIds());
            List<Long> removedTagIds = oldTagIds.stream()
                    .filter(tid -> !newTagIdSet.contains(tid))
                    .toList();
            List<Long> addedTagIds = newTagIdSet.stream()
                    .filter(tid -> !oldTagIds.contains(tid))
                    .toList();
            if (!removedTagIds.isEmpty()) tagRepository.decreaseUsageCountInIds(removedTagIds, 1);
            if (!addedTagIds.isEmpty()) tagRepository.increaseUsageCountInIds(addedTagIds, 1);
            replaceTags(id, new AssignTagsRequest(req.getTagIds()));
        }
        if (req.getCategoryId() != null) {
            Long oldCategoryId = p.getCategory() != null ? p.getCategory().getId() : null;
            if (oldCategoryId == null || !oldCategoryId.equals(req.getCategoryId())) {
                if (oldCategoryId != null) {
                    categoryRepository.decreaseUsageCountById(oldCategoryId, 1);
                }
                categoryRepository.increaseUsageCountById(req.getCategoryId(), 1);
            }
            categoryRepository.findById(req.getCategoryId()).ifPresentOrElse(
                    p::setCategory,
                    () -> { throw new CategoryReferenceInvalidException(req.getCategoryId()); }
            );
        }
        postRepository.save(p);
    }

    @Transactional
    @Override
    public void createPost(CreatePostRequest req) {
        Post p = postMapper.toEntity(req);
        p.setId(IdGenerator.nextPostId());
        p.setSlug(identifierGenerator.fromSlug(req.getSlug(), req.getTitle(), postRepository));
        if (req.getAuthorId() != null) {
            User author = userRepository.findById(req.getAuthorId()).orElseThrow(
                    () -> new NotFoundException("User ID: " + req.getAuthorId())
            );
            p.setAuthor(author);
        }
        if (req.getCategoryId() != null) {
            Category c = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category ID: " + req.getCategoryId()));
            p.setCategory(c);
            categoryRepository.increaseUsageCountById(c.getId(), 1);
        }

        if (req.getCoverageUuid() != null) {
            Resource res = resourceRepository.findByUuidAndStatus(req.getCoverageUuid(), ResourceStatus.ORPHAN)
                    .orElseThrow(() -> new NotFoundException("Resource UUID: " + req.getCoverageUuid()));
            res.setStatus(ResourceStatus.ACTIVE);
            p.setCoverageResource(res);
        }

        postRepository.save(p);

        syncContentResources(p, req.getContent(), null);

        if(CollectionUtil.isNotEmpty(req.getTagIds())) {
            List<Long> distinctTagIds = req.getTagIds().stream().distinct().toList();
            List<Tag> tags = tagRepository.findAllById(distinctTagIds);
            if(CollectionUtil.isNotEmpty(tags)) {
                p.setTags(tags);
                List<PostTag> postTags = tags.stream().map(t -> {
                    PostTag pt = new PostTag();
                    PostTagId pti = new PostTagId();
                    pti.setPostId(p.getId());
                    pti.setTagId(t.getId());

                    pt.setId(pti);
                    pt.setPost(p);
                    pt.setTag(t);
                    return pt;
                }).toList();
                postTagRepository.saveAll(postTags);
                tagRepository.increaseUsageCountInIds(distinctTagIds, 1);
            }
        }
    }

    @Transactional
    @Override
    public BatchResult assignTags(Long postId, AssignTagsRequest req) {
        int success = 0;
        if(CollectionUtil.isNotEmpty(req.getTagIds())) {
            Set<Long> dict = new HashSet<>(req.getTagIds());
            List<Long> existsIds = postTagRepository.findTagIdsByPostId(postId);
            List<Long> validTagIds = tagRepository.findTagByIdIn(dict);

            Set<Long> toSaved = validTagIds.stream()
                    .filter(id -> !existsIds.contains(id))
                    .collect(Collectors.toSet());

            if(CollectionUtil.isNotEmpty(toSaved)) {
                List<PostTag> newPostTags = toSaved.stream().map(tagId -> {
                    PostTag pt = new PostTag();
                    PostTagId pti = new PostTagId();
                    pti.setPostId(postId);
                    pti.setTagId(tagId);

                    pt.setId(pti);
                    pt.setPost(postRepository.getReferenceById(postId));
                    pt.setTag(tagRepository.getReferenceById(tagId));
                    return pt;
                }).toList();
                postTagRepository.saveAll(newPostTags);
                success += newPostTags.size();
            }
        }
        return BatchResult.of(req.getTagIds() == null ? 0 : req.getTagIds().size(), success);
    }

    @Transactional
    @Override
    public BatchResult replaceTags(Long id, AssignTagsRequest req) {
        int removed = postTagRepository.deleteByPostId(id);
        BatchResult adds = BatchResult.empty();
        if(CollectionUtil.isNotEmpty(req.getTagIds())) {
            adds = assignTags(id, req);
        }
        return BatchResult.of(req.getTagIds() == null ? 0 : req.getTagIds().size(), removed + adds.getSuccess());
    }

    @Transactional
    @Override
    public BatchResult deletePosts(DeletePostRequest req) {
        int removed = 0;
        if(CollectionUtil.isNotEmpty(req.getPostIds())) {
            List<Long> distinctIds = req.getPostIds().stream().distinct().toList();
            List<Post> posts = postRepository.findAllById(distinctIds);
            for (Post p : posts) {
                if(p.getCategory() != null){
                    categoryRepository.decreaseUsageCountById(p.getCategory().getId(), 1);
                }
                List<Long> tagIds = p.getTags().stream().map(Tag::getId).toList();
                if(!tagIds.isEmpty()){
                    tagRepository.decreaseUsageCountInIds(tagIds, 1);
                }
            }
            removed += postRepository.deleteByIdIn(distinctIds);
        }
        return BatchResult.ofNullable(req.getPostIds(), removed);
    }

    @Transactional
    @Override
    public BatchResult deletePostTags(Long id, AssignTagsRequest req) {
        int removed = 0;
        if(CollectionUtil.isNotEmpty(req.getTagIds())) {
             removed = postTagRepository.deleteByPostIdAndTagIdIn(id, req.getTagIds());
        }
        return BatchResult.of(req.getTagIds() == null ? 0 : req.getTagIds().size(), removed);
    }

    @Override
    public PostBrief getPostBrief(Long postId) {
        Long authorUid = postRepository.findAuthorUidById(postId);
        UserBrief authorUserBrief = userBriefService.getUserBrief(authorUid);
        Post p = postRepository.findById(postId).orElseThrow(
                () -> new PostReferenceInvalidException(postId)
        );
        return new PostBrief(p.getId(), p.getTitle(), p.getTitle(), authorUserBrief);
    }

    private void syncContentResources(Post p, String newContent, String oldContent) {
        Set<String> newUuids = StringUtil.isBlank(newContent)
                ? Collections.emptySet()
                : StringUtil.extraResPlaceholders(newContent);
        Set<String> oldUuids = StringUtil.isBlank(oldContent)
                ? Collections.emptySet()
                : StringUtil.extraResPlaceholders(oldContent);

        // Activate newly referenced resources
        Set<String> toActivate = new HashSet<>(newUuids);
        toActivate.removeAll(oldUuids);
        if (!toActivate.isEmpty()) {
            resourceRepository.batchUpdateStatusFromTo(ResourceStatus.ORPHAN, ResourceStatus.ACTIVE, toActivate);
        }

        // Sync PostResource links: remove unlinked, add new
        Set<String> toUnlink = new HashSet<>(oldUuids);
        toUnlink.removeAll(newUuids);
        if (!toUnlink.isEmpty()) {
            List<PostResource> toRemove = postResourceRepository
                    .findAllByPostIdAndResourceUuidUuidIn(p.getId(), toUnlink);
            if (!toRemove.isEmpty()) {
                postResourceRepository.deleteAll(toRemove);
            }
        }
        if (!toActivate.isEmpty()) {
            Set<String> existingUuids = postResourceRepository
                    .findByPostId(p.getId()).stream()
                    .map(pr -> pr.getResourceUuid().getUuid())
                    .collect(Collectors.toSet());
            List<PostResource> toSave = toActivate.stream()
                    .filter(uuid -> !existingUuids.contains(uuid))
                    .map(uuid -> {
                        PostResource pr = new PostResource();
                        PostResourceId prId = new PostResourceId(p.getId(), uuid);
                        pr.setId(prId);
                        pr.setPost(p);
                        Resource res = resourceRepository.getReferenceByUuid(uuid);
                        pr.setResourceUuid(res);
                        return pr;
                    })
                    .toList();
            if (!toSave.isEmpty()) {
                postResourceRepository.saveAll(toSave);
            }
        }
    }

    @Override
    public String previewContent(String content) {
        if (StringUtil.isBlank(content)) return content;
        Set<String> uuids = StringUtil.extraResPlaceholders(content);
        if (uuids.isEmpty()) return content;

        List<Resource> resources = resourceRepository.findByUuidIn(uuids).stream()
                .filter(r -> r.getStatus() != ResourceStatus.DELETED)
                .toList();
        Map<String, String> uuidToPath = resources.stream()
                .collect(Collectors.toMap(
                        Resource::getUuid,
                        Resource::getResourceKey
                ));

        Map<String, CloudResPresignedUrlResp> urlMap = cloudFileService.batchGetReadPublicUrlCached(
                CloudFSRoot.SYSTEM,
                uuidToPath,
                TargetType.POST_CONTENT_IMAGE
        );

        Map<String, String> resolved = urlMap.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().getUrl() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getUrl()
                ));

        return StringUtil.replaceResPlaceholders(content, resolved);
    }
}
