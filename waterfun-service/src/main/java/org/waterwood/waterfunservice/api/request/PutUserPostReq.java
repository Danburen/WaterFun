package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PutUserPostReq {
    @Size(max = 32)
    private String title;
    @Size(max = 64)
    private String subtitle;
    private String content;
    @Size(max = 500)
    private String summary;
    private PostVisibility visibility;
    private Long categoryId;
    private Set<Long> tagIds;
}