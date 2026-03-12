package org.waterwood.waterfunadminservice.api.request.perm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.waterfunservicecore.entity.Permission;

import java.io.Serializable;

/**
 * PATCH Update DTO for {@link Permission}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatchPermRequest implements Serializable {
    private String code;
    private String name;
    private String description;
    private PermissionType type;
    private String resource;
    private Integer parentId;
}