package org.waterwood.waterfunservicecore.api.req.auth;

import lombok.Data;

/**
 * Reversed for logout
 * improvement is indeed,implement in the future.
 */
@Data
public class LogoutRequestBody {
    private String deviceId;

}
