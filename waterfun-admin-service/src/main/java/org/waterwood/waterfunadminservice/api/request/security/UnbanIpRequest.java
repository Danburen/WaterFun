package org.waterwood.waterfunadminservice.api.request.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnbanIpRequest {
    @NotBlank
    @Size(max = 45)
    private String ip;
}
