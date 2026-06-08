package org.waterwood.waterfunadminservice.api.response;

import com.ibm.icu.util.LocaleData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

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
