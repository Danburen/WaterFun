package org.waterwood.waterfunservicecore.utils;

import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizUploadPayload {
    private String resourceUuid;
    private String bizType;
    private String bizId;
    private String cosKey;

    public static BizUploadPayload of(Long bizId, String name, UUID uuid) {
        return new BizUploadPayload(
                uuid.toString().replace("-", ""),
                name,
                String.valueOf(bizId),
                null
        );
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if(resourceUuid != null) {
            map.put("resourceUuid", resourceUuid);
        }
        if (bizType != null) {
            map.put("bizType", bizType);
        }
        if (bizId != null) {
            map.put("bizId", bizId);
        }
        if (cosKey != null) {
            map.put("cosKey", cosKey);
        }
        return map;
    }

    public static BizUploadPayload fromMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        BizUploadPayload payload = new BizUploadPayload();
        payload.setBizType(map.get("bizType"));
        payload.setBizId(map.get("bizId"));
        payload.setCosKey(map.get("cosKey"));
        payload.setResourceUuid(map.get("resourceUuid"));
        return payload;
    }
}