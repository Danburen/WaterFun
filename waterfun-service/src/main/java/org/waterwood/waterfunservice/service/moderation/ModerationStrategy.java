package org.waterwood.waterfunservice.service.moderation;

import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;

import java.util.Set;

public interface ModerationStrategy {
    Set<MediaResourceType> getTargetTypes();
    void handle(ModerationConsumerMessage msg);
}
