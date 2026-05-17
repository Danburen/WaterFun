package org.waterwood.waterfunservicecore.utils;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizPayload {
    private String biz;
    private String bizId;
    private String type;
    private String cosKey;

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
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

    public static BizPayload fromMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        BizPayload payload = new BizPayload();
        payload.setBiz(map.get("biz"));
        payload.setBizId(map.get("bizId"));
        payload.setType(map.get("type"));
        payload.setCosKey(map.get("cosKey"));
        return payload;
    }
}