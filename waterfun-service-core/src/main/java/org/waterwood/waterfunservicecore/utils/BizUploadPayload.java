package org.waterwood.waterfunservicecore.utils;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizUploadPayload {
    private String uploadId;
    private String bizType;
    private String bizId;
    private String cosKey;

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if(uploadId != null) {
            map.put("uploadId", uploadId);
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
        payload.setBizType(map.get("biz"));
        payload.setBizId(map.get("bizId"));
        payload.setCosKey(map.get("cosKey"));
        payload.setUploadId(map.get("uploadId"));
        return payload;
    }
}