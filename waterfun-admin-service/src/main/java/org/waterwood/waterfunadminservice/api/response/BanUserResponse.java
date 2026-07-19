package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanUserResponse {
    private Long userUid;
    private String displayName;
    private String nickname;
    private String permissionName;
    private String permissionCode;
    private Instant expiresAt;
    private Instant createdAt;
    private String operatorName;
}
