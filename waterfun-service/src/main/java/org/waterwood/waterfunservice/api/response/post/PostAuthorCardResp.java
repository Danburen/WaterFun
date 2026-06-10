package org.waterwood.waterfunservice.api.response.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;

import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostAuthorCardResp {
    private Long id;
    private String title;
    private String subtitle;
    private String summary;

    private CloudResPresignedUrlResp coverImage;
    private OptionVO<Long> category;
    private List<OptionVO<Long>> tags;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;

    private PostVisibility visibility;
    private PostStatus status;

    private String slug;
    private Instant publishedAt;
}
