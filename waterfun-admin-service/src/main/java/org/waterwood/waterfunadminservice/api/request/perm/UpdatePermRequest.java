package org.waterwood.waterfunadminservice.api.request.perm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.enums.PermissionType;

import java.io.Serializable;

/**
 * DTO for {@link org.waterwood.waterfunservicecore.entity.Permission}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePermRequest implements Serializable {
    private String code;
    private String name;
    private String description;
    private PermissionType type;
    private String resource;
    private Integer parentId;
}