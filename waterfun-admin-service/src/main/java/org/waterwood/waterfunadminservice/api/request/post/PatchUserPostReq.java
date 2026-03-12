package org.waterwood.waterfunadminservice.api.request.post;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.enums.PostVisibility;
import org.waterwood.waterfunservicecore.entity.post.Post;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatchUserPostReq implements Serializable {
    @NotNull
    @Size(max = 32)
    private String title;
    @Size(max = 64)
    private String subtitle;
    @NotNull
    private String content;
    @Size(max = 500)
    private String summary;
    @Size(max = 255)
    private String coverImg;
    private PostVisibility visibility;
    private Integer categoryId;
    private Set<Integer> tagIds;
}