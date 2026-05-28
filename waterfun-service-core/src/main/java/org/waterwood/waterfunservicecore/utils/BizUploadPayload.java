package org.waterwood.waterfunservicecore.utils;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizUploadPayload {
    private String uploadId;
    private String biz;
    private String bizId;
    private String type;
    private String cosKey;

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if(uploadId != null) {
            map.put("uploadId", uploadId);
        }
        if (biz != null) {
            map.put("biz", biz);
        }
        if (bizId != null) {
            map.put("bizId", bizId);
        }
        if (type != null) {
            map.put("type", type);
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
        payload.setBiz(map.get("biz"));
        payload.setBizId(map.get("bizId"));
        payload.setType(map.get("type"));
        payload.setCosKey(map.get("cosKey"));
        payload.setUploadId(map.get("uploadId"));
        return payload;
    }
}