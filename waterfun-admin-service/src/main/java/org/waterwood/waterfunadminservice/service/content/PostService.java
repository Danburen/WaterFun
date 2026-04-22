package org.waterwood.waterfunadminservice.service.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.DeletePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.AssignTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.CreatePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutPostReq;
import org.waterwood.waterfunservicecore.entity.post.Post;

public interface PostService {
    /**
     * List posts ofPending target author for user.
     * @param spec specification ofPending post
     * @param pageable pageable
     * @return Page ofPending Posts
     */
    Page<Post> listPosts(Specification<Post> spec, Pageable pageable);

    /**
     * Get a post
     * @param pid target post id(pid)
     * @return post entity
     * @throws org.waterwood.waterfunservicecore.exception.NotFoundException if post not found
     */
    Post getPostById(Long pid);

    /**
     * Delete a post by pid
     * @param id
     */
    void deletePostById(Long id);

    /**
     * Full upate a role
     * @param id target role id
     * @param req put post request.
     */
    void update(Long id, PutPostReq req);

    /**
     * Create a post
     * @param req request body
     */
    void createPost(CreatePostRequest req);

    /**
     * Assign tags to a post
     *
     * @param id  target post id
     * @param req request body
     * @return batch result ofPending the operation
     */
    BatchResult assignTags(Long id, AssignTagsRequest req);

    /**
     * Replace all tags for a post
     *
     * @param id  target post id
     * @param req requset body
     * @return batch result ofPending the operation
     */
    BatchResult replaceTags(Long id, AssignTagsRequest req);

    /**
     * Batch delete posts
     * @param req request body
     * @return batch result ofPending the operation
     */
    BatchResult deletePosts(DeletePostRequest req);

    /**
     * Batch delete post tags
     * @param id target post id
     * @param req tagIds to be removed
     * @return batch result ofPending the operation
     */
    BatchResult deletePostTags(Long id, AssignTagsRequest req);
}
