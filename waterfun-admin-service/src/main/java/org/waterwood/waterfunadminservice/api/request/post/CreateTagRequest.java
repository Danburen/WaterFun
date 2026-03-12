package org.waterwood.waterfunadminservice.api.request.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTagRequest {
    @NotBlank
    @Size(max = 50)
    String name;
    @Size(max = 50)
    String description;
}
