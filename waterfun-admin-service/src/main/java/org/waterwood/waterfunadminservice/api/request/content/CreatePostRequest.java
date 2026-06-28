package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostType;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.entity.post.Post;

import java.io.Serializable;
import java.util.List;

/**
 * Create Post Request DTO for {@link Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostRequest implements Serializable {
    @Size(max = 32)
    @NotBlank
    private String title;
    @Size(max = 64)
    private String subtitle;
    @NotBlank
    private String content;
    @Size(max = 500)
    private String summary;
    private String coverageUuid;
    @NotNull
    private PostStatus status;
    @NotNull
    private PostVisibility visibility;
    private Long authorId;
    private Long categoryId;
    private String slug;
    private List<Long> tagIds;
    private PostType type = PostType.COMMON;
    private Boolean isPinned = false;
    private Boolean isAnnouncement = false;
}