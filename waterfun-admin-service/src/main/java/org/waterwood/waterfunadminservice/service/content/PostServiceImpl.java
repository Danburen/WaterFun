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
import org.waterwood.waterfunadminservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunadminservice.infrastructure.mapper.RoleMapper;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostTag;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostTagRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.utils.ContentIdGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final RoleMapper roleMapper;
    private final PostMapper postMapper;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final IdentifierGenerator identifierGenerator;
    private final UserRepository userRepository;
    private final PostTagRepository postTagRepository;

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
    public void deletePostById(Long id) {
       postRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void update(Long id, PutPostReq req) {
        Post p = postRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Post ID: " + id)
        );
        postMapper.partialUpdate(req, p);
        if(req.getAuthorId() != null) {
            User u = userRepository.findById(req.getAuthorId()).orElseThrow(
                    () -> new NotFoundException("User ID: " + req.getAuthorId())
            );
            p.setAuthor(u);
        }
        categoryRepository.findById(req.getCategoryId()).ifPresent(p::setCategory);
        postRepository.save(p);
    }

    @Transactional
    @Override
    public void createPost(CreatePostRequest req) {
        Post p = postMapper.toEntity(req);
        p.setId(ContentIdGenerator.nextPostId());
        p.setSlug(identifierGenerator.fromSlug(req.getSlug(), req.getTitle(), postRepository));
        categoryRepository.findById(req.getCategoryId()).map(c -> {
            p.setCategory(c);
            return c;
        }).orElseThrow(() -> new NotFoundException("Category ID: " + req.getCategoryId()));
        postRepository.save(p);

        if(CollectionUtil.isNotEmpty(req.getTagIds())) {
            List<Tag> tags = tagRepository.findAllById(req.getTagIds());
            if(CollectionUtil.isNotEmpty(tags)) {
                p.setTags(tags);
                tags.forEach(t -> {
                    PostTag pt = new PostTag();
                    pt.setPost(p);
                    pt.setTag(t);
                    postTagRepository.save(pt);
                });
            }
        }
    }

    @Override
    public BatchResult assignTags(Long postId, AssignTagsRequest req) {
        int success = 0;
        if(CollectionUtil.isNotEmpty(req.getTagIds())) {
            Set<Integer> dict = new HashSet<>(req.getTagIds());
            List<Integer> existsIds = postTagRepository.findTagIdsByPostId(postId);
            List<Integer> validTagIds = tagRepository.findTagIdsByTagsIdIn(dict);

            Set<Integer> toSaved = validTagIds.stream()
                    .filter(id -> !existsIds.contains(id))
                    .collect(Collectors.toSet());

            if(CollectionUtil.isNotEmpty(toSaved)) {
                List<PostTag> newPostTags = toSaved.stream().map(tagId -> {
                    PostTag pt = new PostTag();
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

    @Override
    public BatchResult replaceTags(Long id, AssignTagsRequest req) {
        int removed = postTagRepository.deleteByPostId(id);
        BatchResult adds = assignTags(id, req);
        return BatchResult.of(req.getTagIds() == null ? 0 : req.getTagIds().size(), removed + adds.getSuccess());
    }

    @Override
    public BatchResult deletePosts(DeletePostRequest req) {
        int removed = 0;
        if(CollectionUtil.isNotEmpty(req.getPostIds())) {
             removed = postRepository.deleteByIdIn(req.getPostIds());
        }
        return BatchResult.of(req.getPostIds() == null ? 0 : req.getPostIds().size(), removed);
    }

    @Override
    public BatchResult deletePostTags(Long id, AssignTagsRequest req) {
        int removed = 0;
        if(CollectionUtil.isNotEmpty(req.getTagIds())) {
             removed = postTagRepository.deleteByPostIdAndTagIdIn(id, req.getTagIds());
        }
        return BatchResult.of(req.getTagIds() == null ? 0 : req.getTagIds().size(), removed);
    }
}
