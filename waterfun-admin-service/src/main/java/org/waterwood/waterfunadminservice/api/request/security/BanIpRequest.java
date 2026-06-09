package org.waterwood.waterfunadminservice.api.request.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanIpRequest {
    @NotBlank
    @Size(max = 45)
    private String ip;

    @Size(max = 100)
    private String reason;

    private Instant expiresAt;
}
