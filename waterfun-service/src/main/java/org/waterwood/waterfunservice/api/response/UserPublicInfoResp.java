package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicInfoResp implements Serializable {
    private Long userUid;
    private String username;
    private CloudResPresignedUrlResp avatar;
    private Instant createdAt;
    private Instant lastActiveAt;
}
