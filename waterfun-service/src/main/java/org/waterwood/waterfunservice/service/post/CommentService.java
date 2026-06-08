package org.waterwood.waterfunservice.service.post;

import jakarta.validation.Valid;
import org.waterwood.waterfunservice.api.request.CreateCommentReq;
import org.waterwood.waterfunservice.api.response.CommentResponse;
import org.waterwood.waterfunservicecore.api.CursorPage;

public interface CommentService {

    /**
     * Create a comment
     * @param req request body
     */
    void create(@Valid CreateCommentReq req);

    /**
     * Delete a comment
     * this will decrease reply count for its parent (if exists)
     * @param id target id
     */
    void delete(Long id);

    /**
     * Like or dislike a comment
     * @param id target comment id
     */
    void like(Long id);

    /**
     * List root comments by cursor
     *
     * @param postId        target post id
     * @param cursor        mixed string  cursor, can be null for first page
     * @param limit         limit of each list
     * @param includeRootId root ids to include in the result, can be null or empty
     * @return {@link CursorPage} of {@link CommentResponse}
     */
    CursorPage<CommentResponse, String> listRootComments(Long postId, String cursor, Integer limit, Long includeRootId);

    /**
     * List second level comments by cursor
     *
     * @param rootId        target root comment id
     * @param cursor        mixed string cursor, can be null for first page
     * @param limit         limit of each list
     * @param includeRootId root ids to include in the result, can be null or empty
     * @return {@link CursorPage} of {@link CommentResponse}
     */
    CursorPage<CommentResponse, Long> listReplies(Long rootId, Long cursor, Integer limit, Long includeRootId);

    /**
     * Get a comment detail
     * @param commentId target comment id
     * @return {@link CommentResponse}
     */
    CommentResponse getComment(Long commentId);
}
