package org.waterwood.waterfunadminservice.api.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.Role;

import java.io.Serializable;

/**
 * Update Role Request DTO for {@link Role}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRoleRequest implements Serializable {
    @Size(max = 50)
    @NotBlank
    private String name;
    @Size(max = 255)
    private String description;
    private Integer parentId;
}