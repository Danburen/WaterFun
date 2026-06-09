package org.waterwood.waterfunservice.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactionInboxPayload implements MultiUserIncludedInboxPayload {
    private List<Long> userUids;
    private Long imageUuid;
    private String nativeUrl;



    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userUids", userUids);
        map.put("imageUuid", imageUuid.toString());
        map.put("nativeUrl", nativeUrl);
        return map;
    }


    @Override
    public MultiUserIncludedInboxPayload withUserUids(LinkedHashSet<Long> userUids) {
        return new ReactionInboxPayload(new ArrayList<>(userUids), imageUuid, nativeUrl);
    }

    @Override
    public MultiUserIncludedInboxPayload formMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return new ReactionInboxPayload();

        Object userIdsObj = map.get("userUids");
        List<Long> userIds = new ArrayList<>();
        if (userIdsObj instanceof List<?>) {
            for (Object id : (List<?>) userIdsObj) {
                userIds.add(((Number) id).longValue());
            }
        }

        return new ReactionInboxPayload(
                userIds,
                Long.parseLong((String) map.get("imageUuid")),
                (String) map.get("nativeUrl")
        );
    }
}
