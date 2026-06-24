package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanStatusResponse {
    private Long userUid;
    private boolean banned;
    private List<ActiveRestriction> restrictions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveRestriction {
        private String permissionCode;
        private String permissionName;
        private String banReasonType;
        private Instant expiresAt;
        private boolean permanent;
        private Instant createdAt;
    }
}
