package org.waterwood.waterfunservicecore.api.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModerationBatchMessage {
    private List<ModerationConsumerMessage> items;
    private Instant sendTime = Instant.now();
    private int total;

    public ModerationBatchMessage(List<ModerationConsumerMessage> items) {
        this.items = items;
        this.total = items.size();
    }
}