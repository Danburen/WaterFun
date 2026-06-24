package org.waterwood.waterfunadminservice.service.content;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunadminservice.api.request.DeletePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.AssignTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.CreatePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutPostReq;
import org.waterwood.waterfunadminservice.api.response.content.audit.PostBrief;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
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
import org.waterwood.waterfunservicecore.infrastructure.persistence.*;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.IdGenerator;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final UserBriefService userBriefService;

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

    @Override
    @Transactional
    public void deletePostById(Long id) {
        postTagRepository.deleteByPostId(id);
        int removed = postRepository.deleteByIdIn(List.of(id));
        if (removed == 0) {
            throw new NotFoundException("Post ID: " + id);
        }
    }

    @Transactional
    @Override
    public void update(Long id, PutPostReq req) {
        Post p = postRepository.findById(id).orElseThrow(
                PostNotFoundException::new
        );
        postMapper.partialUpdate(req, p);
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
        } else {
            oldRes.setStatus(ResourceStatus.ORPHAN);
            Resource res = resourceRepository.findByUuidAndStatus(req.getCoverageUuid(), ResourceStatus.ORPHAN)
                    .orElseThrow(() -> new NotFoundException(req.getCoverageUuid()));
            res.setStatus(ResourceStatus.ACTIVE);
            p.setCoverageResource(res);
        }

        if(req.getTagIds() != null) {
            replaceTags(id, new AssignTagsRequest(req.getTagIds()));
        }
        categoryRepository.findById(req.getCategoryId()).ifPresentOrElse(
                p::setCategory,
                () -> {
                    if (req.getCategoryId() != null) {
                        throw new CategoryReferenceInvalidException(req.getCategoryId());
                    }
                }
        );
        postRepository.save(p);
    }

    @Transactional
    @Override
    public void createPost(CreatePostRequest req) {
        Post p = postMapper.toEntity(req);
        p.setId(IdGenerator.nextPostId());
        p.setSlug(identifierGenerator.fromSlug(req.getSlug(), req.getTitle(), postRepository));
        User author = userRepository.findById(req.getAuthorId()).orElseThrow(
                () -> new NotFoundException("User ID: " + req.getAuthorId())
        );
        p.setAuthor(author);
        categoryRepository.findById(req.getCategoryId()).map(c -> {
            p.setCategory(c);
            return c;
        }).orElseThrow(() -> new NotFoundException("Category ID: " + req.getCategoryId()));

        if (req.getCoverageUuid() != null) {
            Resource res = resourceRepository.findByUuidAndStatus(req.getCoverageUuid(), ResourceStatus.ORPHAN)
                    .orElseThrow(() -> new NotFoundException("Resource UUID: " + req.getCoverageUuid()));
            res.setStatus(ResourceStatus.ACTIVE);
            p.setCoverageResource(res);
        }

        postRepository.save(p);

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
            List<Long> validTagIds = tagRepository.findTagIdsByTagsIdIn(dict);

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
            for (Long postId : distinctIds) {
                postTagRepository.deleteByPostId(postId);
                removed += postRepository.deleteByIdIn(List.of(postId));
            }
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
}
