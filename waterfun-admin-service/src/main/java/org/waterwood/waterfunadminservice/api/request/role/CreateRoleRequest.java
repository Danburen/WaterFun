package org.waterwood.waterfunadminservice.api.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.user.Role;

import java.io.Serializable;

/**
 * DTO for {@link Role}
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
    @Size(max = 50)
    private String code;
    private Integer orderWeight;
    private Boolean isSystem;
}