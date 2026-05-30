package org.waterwood.waterfunservice.api.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.post.Post;

import java.util.Set;

/**
 * Post save Request DTO for {@link Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostSaveReq {
    @Size(max = 32)
    @NotBlank
    private String title;
    @Size(max = 64)
    private String subtitle;
    @NotBlank
    private String content;
    @Size(max = 500)
    private String summary;
    @Size(max = 36)
    private String coverageImgId;

    private Set<String> newTags;
    private Set<Long> tagIds;
    @NotNull
    private Long categoryId;
}