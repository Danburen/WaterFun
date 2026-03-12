package org.waterwood.waterfunadminservice.api.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for {@link org.waterwood.waterfunservicecore.entity.Role}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRoleRequest implements Serializable {
    @Size(max = 50)
    @NotBlank
    private String name;
    @Size(max = 255)
    private String description;
    private Integer parentId;
}