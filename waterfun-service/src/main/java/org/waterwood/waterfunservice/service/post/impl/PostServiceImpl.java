package org.waterwood.waterfunservice.service.post.impl;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.common.CloudStorageRootKey;
import org.waterwood.utils.codec.Snowflake;
import org.waterwood.waterfunservice.api.response.post.PostAuthorCardResp;
import org.waterwood.waterfunservice.api.response.post.PostAuthorDetailResp;
import org.waterwood.waterfunservice.api.response.post.PostCardResp;
import org.waterwood.waterfunservice.api.response.post.PostDetailResp;
import org.waterwood.waterfunservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.TagRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservice.service.post.CategoryService;
import org.waterwood.waterfunservice.service.post.PostService;
import org.waterwood.waterfunservice.service.post.TagService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunservicecore.utils.ContentIdGenerator;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final IdentifierGenerator identifierGenerator;
    private final UserCoreService userCoreService;
    private final TagRepository tagRepository;
    private final TagService tagService;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final CloudFileService cloudFileService;
    private final PostMapper postMapper;
    private final MessageSource messageSource;

    public PostServiceImpl(PostRepository postRepository, IdentifierGenerator identifierGenerator, UserRepository userRepository, UserCoreService userCoreService, TagRepository tagRepository, TagService tagService, CategoryService categoryService, CategoryRepository categoryRepository, CloudFileService cloudFileService, PostMapper postMapper, MessageSource messageSource) {
        this.postRepository = postRepository;
        this.identifierGenerator = identifierGenerator;
        this.userCoreService = userCoreService;
        this.tagRepository = tagRepository;
        this.tagService = tagService;
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.cloudFileService = cloudFileService;
        this.postMapper = postMapper;
        this.messageSource = messageSource;
    }

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
    public void updatePost(Post post, Set<Integer> tagIds, Integer categoryId) {
        List<Tag> tags =tagRepository.findAllById(tagIds);
        Category category = categoryService.getCategory(categoryId);
        User u = userCoreService.getUserByUid(UserCtxHolder.getUserUid());
        post.setCategory(category);
        post.setAuthor(u);
        post.setSlug(identifierGenerator.generateSlug(post.getTitle(), postRepository));
        post.setTags(tags);
        postRepository.save(post);
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
        p.setTitle(messageSource.getMessage(
                "post.title.draft.untitled",
                null,
                "Untitled Post",
                UserCtxHolder.getLocale()));
        postRepository.save(p);
        return id;
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
                CloudStorageRootKey.UPLOADS,
                post.getCoverImg(),
                post.getId(),
                MediaResourceType.COVERAGE
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
                CloudStorageRootKey.UPLOADS,
                posts.stream()
                        .map(Post::getCoverImg)
                        .toList(),
                postIds,
                MediaResourceType.COVERAGE);

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
}
