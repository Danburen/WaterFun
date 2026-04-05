package org.waterwood.waterfunadminservice.api.response.role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.Role;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link Role}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleResp implements Serializable {
    private Integer id;
    private String code;
    private String name;
    private Integer orderWeight;
    private String description;
    private Integer parentId;
    private Boolean isSystem;
    private Instant createdAt;
    private Instant updatedAt;
}