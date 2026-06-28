package org.waterwood.waterfunadminservice.api.response.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostType;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.entity.post.Post;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse implements Serializable {
    private Long id;
    private String title;
    private String subtitle;
    private String content;
    private String contentHtml;
    private String summary;
    private Long authorId;
    private String coverImg;
    private CloudResPresignedUrlResp coverImage;
    private PostStatus status;
    private PostVisibility visibility;
    private Long categoryId;
    private List<Long> tagIds;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;
    private String slug;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private PostType type = PostType.COMMON;
    private Boolean isPinned = false;
    private Boolean isAnnouncement = false;
    private String editedTitle;
    private String editedSubtitle;
    private String editedContent;
    private String editedContentHtml;
    private String editedSummary;
    private String editedCoverImg;
    private Long editedCategoryId;
    private List<Long> editedTagIds;
}