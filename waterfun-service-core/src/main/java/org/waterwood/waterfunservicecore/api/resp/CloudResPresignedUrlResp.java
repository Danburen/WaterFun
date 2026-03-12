package org.waterwood.waterfunservicecore.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
public class CloudResPresignedUrlResp implements Serializable {
    private String url;
    private Instant expireAt;
}
