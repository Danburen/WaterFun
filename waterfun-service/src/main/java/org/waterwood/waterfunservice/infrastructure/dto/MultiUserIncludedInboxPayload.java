package org.waterwood.waterfunservice.infrastructure.dto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public interface MultiUserIncludedInboxPayload extends InboxPayload {
    List<Long> getUserUids();
    MultiUserIncludedInboxPayload withUserUids(LinkedHashSet<Long> userUids);
    MultiUserIncludedInboxPayload formMap(Map<String, Object> map);
}
