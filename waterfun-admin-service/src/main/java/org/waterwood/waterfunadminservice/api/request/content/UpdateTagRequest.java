package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTagRequest implements Serializable {
    @NotNull
    Long id;
    @Size(max = 50)
    String name;
    String description;
}
