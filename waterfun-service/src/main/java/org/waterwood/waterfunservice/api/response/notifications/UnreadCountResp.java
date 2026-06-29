package org.waterwood.waterfunservice.api.response.notifications;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnreadCountResp {
    private int total;
    private Map<String, Long> tabs;
}
