package org.waterwood.waterfunadminservice.api.request.perm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.waterwood.api.enums.PermissionType;

import java.io.Serializable;

@Data
public class CreatePermRequest implements Serializable {
    @NotBlank
    @Size(max = 50)
    private String code;
    @NotBlank
    @Size(max = 50)
    private String name;
    @Size(max = 255)
    private String description;
    private PermissionType type;
    @NotBlank
    @Size(max = 255)
    private String resource;
    private Integer parentId;
}
