package org.waterwood.waterfunservicecore.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.HttpMethod;

import java.io.Serializable;
import java.time.Instant;

/**
 * Upload response for STS-constrained direct upload.
 */
@Data
@AllArgsConstructor
public class StsUploadPolicyResp implements Serializable {
    private String key;
    private String url;
    private HttpMethod method;
    private long maxContentLength;
    private String allowedContentType;
    private Instant expiresAt;
}

