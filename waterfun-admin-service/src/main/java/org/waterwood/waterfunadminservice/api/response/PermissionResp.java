package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.enums.PermissionType;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link org.waterwood.waterfunservicecore.entity.Permission}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionResp implements Serializable {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private PermissionType type;
    private String resource;
    private Integer parentId;
    private Instant createdAt;
}