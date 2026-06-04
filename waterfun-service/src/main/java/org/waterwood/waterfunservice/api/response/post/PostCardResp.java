package org.waterwood.waterfunservice.api.response.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservice.api.response.UserBrief;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.post.Post;

import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostCardResp {
    private Long id;
    private String title;
    private String subtitle;
    private String summary;

    private UserBrief userBrief;

    private CloudResPresignedUrlResp coverImage;
    private OptionVO<Integer> category;
    private List<OptionVO<Integer>> tags;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;

    private String slug;
    private Instant publishedAt;
}