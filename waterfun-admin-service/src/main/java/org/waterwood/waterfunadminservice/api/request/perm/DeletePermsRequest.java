package org.waterwood.waterfunadminservice.api.request.perm;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletePermsRequest {
    @NotEmpty
    @Size(max = 1000)
    private List<Integer> permIds;
}
