package org.waterwood.waterfunadminservice.api.response.content;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.io.Serializable;
import java.time.Instant;

/**
 * Response DTO for {@link Category}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse implements Serializable {
    private Integer id;
    private String name;
    private String slug;
    private String description;
    private Long parentId;
    private Integer sortOrder;
    private Boolean isActive;
    private Long creatorId;
    Instant updateAt;
    Instant createdAt;
}