package org.waterwood.waterfunservicecore.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.HttpMethod;

import java.io.Serializable;

/**
 * File upload response dto for file upload sign to cloud temporary storage.
 */
@Data
@AllArgsConstructor
public class PresignedResp implements Serializable {
    /**
     * path key without prefix
     */
    private String key;
    private String url;
    private HttpMethod method;
    private String token;
}
