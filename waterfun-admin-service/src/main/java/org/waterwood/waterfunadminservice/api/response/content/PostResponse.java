package org.waterwood.waterfunadminservice.api.response.content;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.enums.PostStatus;
import org.waterwood.api.enums.PostVisibility;
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
    private String summary;
    private Long authorId;
    private String coverImg;
    private PostStatus status;
    private PostVisibility visibility;
    private Integer categoryId;
    private List<Integer> tagIds;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;
    private String slug;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
}