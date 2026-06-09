package org.waterwood.waterfunservice.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowerInboxPayload implements InboxPayload {
    private Long followerUid;
    private String nativeUrl;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("followerUid", followerUid);
        map.put("nativeUrl", nativeUrl);
        return map;
    }
}
