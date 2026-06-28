package org.waterwood.waterfunadminservice.confirguation;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.waterwood.common.RabbitConstants;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange moderationExchange() {
        return new DirectExchange(RabbitConstants.MODERATION_EXCHANGE);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Queue moderationNotificationQueue() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_MODERATION_NOTIFICATION)
                .quorum()
                .withArgument("x-delivery-limit", 3)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", RabbitConstants.QUEUE_MODERATION_NOTIFICATION + ".dlq")
                .build();
    }

    @Bean
    public Queue moderationDlq() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_MODERATION_NOTIFICATION + ".dlq")
                .quorum()
                .build();
    }

    @Bean
    public Binding moderationBinding() {
        return BindingBuilder
                .bind(moderationNotificationQueue())
                .to(moderationExchange())
                .with(RabbitConstants.ROUTE_MODERATION_RESULT);
    }

    @Bean
    public Binding moderationBatchBinding() {
        return BindingBuilder
                .bind(moderationNotificationQueue())
                .to(moderationExchange())
                .with(RabbitConstants.ROUTE_MODERATION_BATCH_RESULT);
    }

    @Bean
    public Queue ticketNotificationQueue() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_TICKET_NOTIFICATION)
                .quorum()
                .withArgument("x-delivery-limit", 3)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", RabbitConstants.QUEUE_TICKET_NOTIFICATION + ".dlq")
                .build();
    }

    @Bean
    public Queue ticketDlq() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_TICKET_NOTIFICATION + ".dlq")
                .quorum()
                .build();
    }

    @Bean
    public Binding ticketResultBinding() {
        return BindingBuilder
                .bind(ticketNotificationQueue())
                .to(moderationExchange())
                .with(RabbitConstants.ROUTE_TICKET_RESULT);
    }
}
