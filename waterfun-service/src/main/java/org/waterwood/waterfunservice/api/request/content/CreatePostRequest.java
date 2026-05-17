package org.waterwood.waterfunservice.api.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.infrastructure.validation.PostState;

import java.io.Serializable;
import java.util.Set;

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
    @PostState
    private String status;
    private Set<Integer> tagIds;
}