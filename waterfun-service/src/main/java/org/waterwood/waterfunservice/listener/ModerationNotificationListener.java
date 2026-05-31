package org.waterwood.waterfunservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.waterwood.common.RabbitConstants;
import org.waterwood.waterfunservice.service.moderation.ModerationCallbackStrategy;
import org.waterwood.waterfunservice.service.moderation.ModerationCallbackStrategyFactory;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationNotificationListener {

    private final ModerationCallbackStrategyFactory moderationCallbackStrategyFactory;

    @RabbitListener(queues = RabbitConstants.QUEUE_MODERATION_NOTIFICATION)
    public void handleModerationResult(ModerationConsumerMessage msg){
        ModerationCallbackStrategy strategy = moderationCallbackStrategyFactory.getStrategy(msg.getTargetType());
        strategy.handle(msg);
    }
}
