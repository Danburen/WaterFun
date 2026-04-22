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
    private String name;

    @Size(max = 50)
    private String slug;

    @Size(max = 500)
    private String description;

    private Integer parentId;

    private Integer sortOrder;

    private Boolean isActive;
}
