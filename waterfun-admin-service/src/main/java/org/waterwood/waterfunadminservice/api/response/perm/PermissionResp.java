package org.waterwood.waterfunadminservice.api.response.perm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.perm.PermissionType;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link Permission}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionResp implements Serializable {
    private Integer id;
    private String code;
    private String name;
    private Integer orderWeight;
    private String description;
    private PermissionType type;
    private String resource;
    private Boolean isSystem;
    private Integer parentId;
    private Instant createdAt;
}