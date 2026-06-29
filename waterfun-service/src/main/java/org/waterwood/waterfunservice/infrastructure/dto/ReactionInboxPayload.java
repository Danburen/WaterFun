package org.waterwood.waterfunservice.infrastructure.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class ReactionInboxPayload implements MultiUserIncludedInboxPayload {
    private List<Long> userUids;
    private String postCoverageResUuid;
    private String nativeUrl;

    public ReactionInboxPayload(List<Long> userUids, String postCoverageResUuid, String nativeUrl) {
        this.userUids = userUids;
        this.postCoverageResUuid = postCoverageResUuid;
        this.nativeUrl = nativeUrl;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userUids", userUids);
        if (postCoverageResUuid != null) {
            map.put("postCoverageResUuid", postCoverageResUuid);
        }
        map.put("nativeUrl", nativeUrl);
        return map;
    }

    @Override
    public MultiUserIncludedInboxPayload withUserUids(LinkedHashSet<Long> userUids) {
        return new ReactionInboxPayload(new ArrayList<>(userUids), postCoverageResUuid, nativeUrl);
    }

    @Override
    public MultiUserIncludedInboxPayload formMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return new ReactionInboxPayload();

        Object userIdsObj = map.get("userUids");
        List<Long> userIds = new ArrayList<>();
        if (userIdsObj instanceof List<?>) {
            for (Object id : (List<?>) userIdsObj) {
                if (id instanceof Number n) userIds.add(n.longValue());
                else if (id instanceof String s) { try { userIds.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {} }
            }
        }

        Object coverageVal = map.get("postCoverageResUuid");
        return new ReactionInboxPayload(
                userIds,
                coverageVal instanceof String s ? s : (coverageVal == null ? null : coverageVal.toString()),
                (String) map.get("nativeUrl")
        );
    }
}
