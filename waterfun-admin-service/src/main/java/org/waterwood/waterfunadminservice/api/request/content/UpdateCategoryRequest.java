package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.io.Serializable;

/**
 * Update Category Request DTO for {@link Category}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryRequest implements Serializable {
    @Size(max = 50)
    private String name;
    @Size(max = 50)
    private String slug;
    private String description;
    private Integer parentId;
    private Integer sortOrder;
    private Boolean isActive;
}
