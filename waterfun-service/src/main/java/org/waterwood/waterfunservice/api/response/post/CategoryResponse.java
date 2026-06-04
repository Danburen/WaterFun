package org.waterwood.waterfunservice.api.response.post;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.VO.OptionVO;
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
    @NotNull
    Long id;
    @Size(max = 50)
    @NotEmpty
    String name;
    @Size(max = 50)
    @NotEmpty
    String slug;
    String description;
    OptionVO<Long> parent;
    Integer sortOrder;
    Boolean isActive;
    Long usageCount;
    @NotNull
    Instant createdAt;
}