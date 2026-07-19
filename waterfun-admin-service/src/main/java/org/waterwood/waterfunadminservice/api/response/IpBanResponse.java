package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpBanResponse {
    private Long id;
    private String ip;
    private String reason;
    private Instant bannedAt;
    private Instant expiresAt;
}
