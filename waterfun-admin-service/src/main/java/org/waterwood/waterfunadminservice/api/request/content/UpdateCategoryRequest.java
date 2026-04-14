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
    @NotNull
    Long id;
    @Size(max = 50)
    String name;
    @Size(max = 50)
    String slug;
    String description;
    Long parentId;
    Integer sortOrder;
    Boolean isActive;
}
