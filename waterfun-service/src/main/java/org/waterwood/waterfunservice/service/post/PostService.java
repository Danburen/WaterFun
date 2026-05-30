package org.waterwood.waterfunservice.service.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservice.api.UploadContext;
import org.waterwood.waterfunservice.api.request.PutUserPostReq;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservice.api.response.post.*;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.post.Post;

import java.util.List;
import java.util.Set;

public interface PostService {
    /**
     * Add a post
     * @param entity post entity ofPending {@link  Post} to add
     */
    void add(Post entity, Set<Long> tagIds);

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
     * Draft a new post and return the post id
     *
     * @return long value of generated new post id
     */
    Long draftNew();

    /**
     * Update a post must at DRAFT status by user self
     * @param id target post id
     * @param req request body
     */
    void updatePost(Long id, PutUserPostReq req);

    /**
     * User publish self post
     *
     * @param id  target post id
     * @param req post save request body
     */
    void publish(Long id, PostSaveReq req);

    /**
     * Handle post coverage image upload
     * @param request upload policy request body
     * @return List of presigned resp usually only one element
     */
    List<PresignedResp> handlePostCoverageImageUpload(UploadPolicyReq request);

    /**
     * Hande post content images upload
     * @param request upload policy request body
     * @return List of presigned resp
     */
    List<PresignedResp> handlePostContentImageUpload(UploadPolicyReq request);

    /**
     * Handle post content image upload callback
     * @param request request body
     * @param ctx business upload context resolved from stored payload
     */
    void handlePostImageUploadCallback(CloudPutCallbackReq request, UploadContext<Long> ctx);

    /**
     * Temporarily save the post content, which is in DRAFT status.
     *
     * @param id      target id
     * @param request request body to save
     */
    void save(Long id, PostSaveReq request);

    /**
     * Preview a post content
     * must be user self post,
     * and the content is saved by {@link #save(Long, PostSaveReq)}
     *
     * @param id target post id context
     * @param content content to preview could be any content
     * @return rendered string with placed resource url
     */
    String contentPreview(Long id, String content);

    
    PostDraftResp getEditPostDraft(Long id);
}
