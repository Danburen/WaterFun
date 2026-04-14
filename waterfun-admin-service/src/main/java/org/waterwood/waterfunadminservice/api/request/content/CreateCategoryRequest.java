package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.io.Serializable;

/**
 * Create DTO for {@link Category}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryRequest implements Serializable {
    @Size(max = 50)
    @NotBlank
    String name;
    String description;
    Long parentId;
    Integer sortOrder;
    Boolean isActive;
}
