package org.waterwood.waterfunservice.service.post;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
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
     * User publish self post
     *
     * @param id  target post id
     * @param req post save request body
     */
    void publish(Long id, PostSaveReq req);

    /**
     * Handle post coverage image upload
     * if {@link UserUploadPolicyReq#getBizId()} is provided, then the image will be associated with the post id in bizId,
     * if not, will use Current user uid as biz id, means the resource is associated with target user
     * @param request upload policy request body
     * @return List of presigned resp usually only one element
     */
    List<PresignedResp> handlePostCoverageImageUpload(UserUploadPolicyReq request);

    /**
     * Hande post content images upload
     * if {@link UserUploadPolicyReq#getBizId()} is provided, then the image will be associated with the post id in bizId,
     * if not, will use Current user uid as biz id, means the resource is associated with target user
     * @param request upload policy request body
     * @return List of presigned resp
     */
    List<PresignedResp> handlePostContentImageUpload(UserUploadPolicyReq request);

    /**
     * Handle post content image upload callback
     * @param request request body
     * @param ctx business upload context resolved from stored payload
     */
    void handlePostImageUploadCallback(CloudPutCallbackReq request, UserUploadContext<Long> ctx);

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

    /**
     * User like a post
     * @param id target post id
     */
    void like(Long id);

    /**
     * User collection a post
     * @param id target post id
     */
    void collection(Long id);

    /**
     * Draft a new post and publish directly, which means the post will be created and published immediately.
     * @param req {@link PostSaveReq}
     */
    void publishNewPost(@Valid PostSaveReq req);

    /**
     * Draft a new post and temp-save it
     * @param req {@link  PostSaveReq}
     */
    void saveNewPost(@Valid PostSaveReq req);
}
