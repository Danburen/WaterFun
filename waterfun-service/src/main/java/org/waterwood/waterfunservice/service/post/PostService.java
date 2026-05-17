package org.waterwood.waterfunservice.service.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservice.api.response.post.PostAuthorCardResp;
import org.waterwood.waterfunservice.api.response.post.PostAuthorDetailResp;
import org.waterwood.waterfunservice.api.response.post.PostCardResp;
import org.waterwood.waterfunservice.api.response.post.PostDetailResp;
import org.waterwood.waterfunservicecore.entity.post.Post;

import java.util.Set;

public interface PostService {
    /**
     * Add a post
     * @param entity post entity ofPending {@link  Post} to add
     */
    void add(Post entity, Set<Integer> tagIds);

    /**
     * List posts ofPending target author for user.
     * @param spec query specification
     * @param pageable pageable
     * @return  posts
     */
    Page<Post> listPosts(Specification<Post> spec, Pageable pageable);

    /**
     * Delete current user's post
     * @param id post id
     */
    void deletePost(Long id);

    /**
     * Get post entity by id
     * @param id post id
     * @return post entity
     */
    Post getPostById(Long id);

    /**
     * Update post
     *
     * @param p          post entity
     * @param tagIds     tag ids
     * @param categoryId category id
     */
    void updatePost(Post p, Set<Integer> tagIds, Integer categoryId);

    Page<PostCardResp> listCardPosts(Specification<Post> spec, Pageable pageable);

    Page<PostAuthorCardResp> listAuthorCardPosts(Specification<Post> spec, Pageable pageable);

    /**
     * Get a post public detail
     * @param id target id
     * @return post detail resp
     */
    PostDetailResp getPostDetail(Long id);

    PostAuthorDetailResp getSelfPostDetail(Long id);

    /**
     * Draft a new post
     *
     * @return long value of generated new post id
     */
    Long draftNew();
}
