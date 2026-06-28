package org.waterwood.waterfunservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.waterwood.common.RabbitConstants;
import org.waterwood.waterfunservice.service.moderation.ModerationCallbackStrategy;
import org.waterwood.waterfunservice.service.moderation.ModerationCallbackStrategyFactory;
import org.waterwood.waterfunservicecore.api.message.ModerationBatchMessage;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@RabbitListener(queues = RabbitConstants.QUEUE_MODERATION_NOTIFICATION, containerFactory = "rabbitListenerContainerFactory")
public class AuditModerationListener {

    private final ModerationCallbackStrategyFactory moderationCallbackStrategyFactory;


    @RabbitHandler
    public void handleModerationResult(ModerationConsumerMessage msg){
        ModerationCallbackStrategy strategy = moderationCallbackStrategyFactory.getStrategy(msg.getTargetType());
        strategy.handle(msg);
    }

    @RabbitHandler
    public void handleBatchModerationCallback(ModerationBatchMessage batchMessage) {
        Map<TargetType, List<ModerationConsumerMessage>> grouped = batchMessage.getItems().stream()
                .collect(Collectors
                        .groupingBy(ModerationConsumerMessage::getTargetType)
                );

        for (Map.Entry<TargetType, List<ModerationConsumerMessage>> entry : grouped.entrySet()) {
            ModerationCallbackStrategy strategy = moderationCallbackStrategyFactory.getStrategy(entry.getKey());
            strategy.handleBatch(entry.getValue());
        }
    }
}
