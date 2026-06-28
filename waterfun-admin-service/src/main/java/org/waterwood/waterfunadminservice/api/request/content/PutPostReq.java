package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostType;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link org.waterwood.waterfunservicecore.entity.post.Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PutPostReq implements Serializable {
    @NotNull
    @Size(max = 32)
    private String title;
    @Size(max = 64)
    private String subtitle;
    @NotNull
    private String content;
    @Size(max = 500)
    private String summary;
    private String coverageUuid;
    private PostStatus status = PostStatus.DRAFT;
    private PostVisibility visibility = PostVisibility.PUBLIC;
    private Long authorId;
    @Size(max = 200)
    private String slug;
    private Long categoryId;
    private List<Long> tagIds;
    private PostType type;
    private Boolean isPinned;
    private Boolean isAnnouncement;
}