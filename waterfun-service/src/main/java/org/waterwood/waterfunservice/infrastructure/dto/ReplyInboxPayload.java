package org.waterwood.waterfunservice.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyInboxPayload implements MultiUserIncludedInboxPayload {
    private List<Long> userUids;
    private String replyContent;
    private String nativeUrl;
    private String postCoverageResUuid;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userUids", userUids);
        map.put("replyContent", replyContent);
        map.put("nativeUrl", nativeUrl);
        if (postCoverageResUuid != null) {
            map.put("postCoverageResUuid", postCoverageResUuid);
        }
        return map;
    }

    @Override
    public MultiUserIncludedInboxPayload withUserUids(LinkedHashSet<Long> userUids) {
        return new ReplyInboxPayload(new ArrayList<>(userUids), replyContent, nativeUrl, postCoverageResUuid);
    }

    @Override
    public MultiUserIncludedInboxPayload formMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return new ReplyInboxPayload();
        Object userIdsObj = map.get("userUids");
        List<Long> userIds = new ArrayList<>();
        if (userIdsObj instanceof List<?>) {
            for (Object id : (List<?>) userIdsObj) {
                if (id instanceof Number n) userIds.add(n.longValue());
                else if (id instanceof String s) { try { userIds.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {} }
            }
        }
        return new ReplyInboxPayload(
                userIds,
                (String) map.get("replyContent"),
                (String) map.get("nativeUrl"),
                (String) map.get("postCoverageResUuid")
        );
    }
}
