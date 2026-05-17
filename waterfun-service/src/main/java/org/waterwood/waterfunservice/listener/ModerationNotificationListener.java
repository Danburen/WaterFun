package org.waterwood.waterfunservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.waterwood.common.RabbitConstants;
import org.waterwood.waterfunservice.service.moderation.ModerationStrategy;
import org.waterwood.waterfunservice.service.moderation.ModerationStrategyFactory;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationNotificationListener {

    private final ModerationStrategyFactory moderationStrategyFactory;

    @RabbitListener(queues = RabbitConstants.QUEUE_MODERATION_NOTIFICATION)
    public void handleModerationResult(ModerationConsumerMessage msg){
        ModerationStrategy strategy = moderationStrategyFactory.getStrategy(msg.getTargetType());
        strategy.handle(msg);
    }
}
