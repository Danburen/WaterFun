package org.waterwood.waterfunservice.service.moderation;

import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

import java.util.List;
import java.util.Set;

public interface ModerationCallbackStrategy {
    Set<TargetType> getTargetTypes();
    void handle(ModerationConsumerMessage msg);
    void handleBatch(List<ModerationConsumerMessage> msgs);
}
