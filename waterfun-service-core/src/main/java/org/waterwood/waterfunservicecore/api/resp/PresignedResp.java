package org.waterwood.waterfunservicecore.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.HttpMethod;

import java.io.Serializable;

/**
 * File upload response dto for file upload sign to cloud temporary storage.
 * token is usually same as resource uuid
 */
@Data
@AllArgsConstructor
public class PresignedResp implements Serializable {
    private String url;
    private HttpMethod method;
    private String token;
    private Boolean success = true;
    private String errorMsg = null;

    public PresignedResp(String url, HttpMethod method, String token) {
        this.url = url;
        this.method = method;
        this.token = token;
    }

    public static PresignedResp ofError(String errorMsg) {
        return new PresignedResp(
                null,
                null,
                null,
                false,
                errorMsg
        );
    }
}
