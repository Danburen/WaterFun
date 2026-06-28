package org.waterwood.waterfunservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.waterwood.common.RabbitConstants;
import org.waterwood.waterfunservice.service.moderation.TicketResultHandler;
import org.waterwood.waterfunservicecore.api.message.TicketMessage;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitConstants.QUEUE_TICKET_NOTIFICATION, containerFactory = "rabbitListenerContainerFactory")
public class TicketResultListener {

    private final TicketResultHandler ticketResultHandler;

    @RabbitHandler
    public void handleTicketResult(TicketMessage msg) {
        ticketResultHandler.handle(msg);
    }
}
