package org.waterwood.waterfunservicecore.api.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class DeviceInfoReq {
    @NotBlank(message = "{auth.device_fingerprint.required}")
    private String deviceFp;
    private String os;
    private String browser;

    public Map<String, Object> toMap() {
        return Map.of(
                "deviceFp", deviceFp,
                "os", os != null ? os : "",
                "browser", browser != null ? browser : ""
        );
    }
}
