package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTagRequest implements Serializable {
    @NotBlank
    @Size(max = 30)
    private String name;

    @Size(max = 50)
    private String slug;

    @Size(max = 500)
    private String description;
}
