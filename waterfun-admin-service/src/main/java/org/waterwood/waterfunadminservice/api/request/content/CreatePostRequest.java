package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.waterwood.api.enums.PostStatus;
import org.waterwood.api.enums.PostVisibility;
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
    @Size(max = 255)
    @URL
    private String coverImg;
    @NotNull
    private PostStatus status;
    @NotNull
    private PostVisibility visibility;
    @NotNull
    private Long authorId;
    @NotNull
    private Integer categoryId;
    private String slug;
    private List<Integer> tagIds;
}