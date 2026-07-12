package org.waterwood.waterfunservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationDlqListener {

    @RabbitListener(queues = "notification.moderation.dlq")
    public void handleDlqMessage(Message message) {
        log.error("Moderation notification message landed in DLQ (all retries exhausted). " +
                        "Headers: {}, Body preview: {}",
                message.getMessageProperties().getHeaders(),
                message.getBody() != null ? new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8).substring(0, Math.min(500, message.getBody().length)) : "empty"
        );
    }
}
