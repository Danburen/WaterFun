package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;

import java.time.Instant;

/**
 * DTO for {@link org.waterwood.waterfunservicecore.entity.post.Comment}
 */
@AllArgsConstructor
@Getter
public class CommentResponse {
    private final Long id;
    private final Long postId;
    private final Long parentId;
    private final Long rootId;
    private final UserBrief author;

    private final String content;
    private final Long likeCount;
    private final Long replyCount;
    private final Instant createdAt;
    private final String replyToDisplayName;
}