package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTagReq {
    @Size(max = 30)
    private String name;
    @Size(max = 50)
    private String slug;
    private String description;
}
