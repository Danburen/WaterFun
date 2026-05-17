package org.waterwood.waterfunservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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
    public Queue moderationNotificationQueue() {
        return QueueBuilder.durable(RabbitConstants.QUEUE_MODERATION_NOTIFICATION)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", RabbitConstants.QUEUE_MODERATION_NOTIFICATION + ".dlq")
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
    public Queue moderationDlq() {
        return new Queue(RabbitConstants.QUEUE_MODERATION_NOTIFICATION + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Use JSON conversion instead of Java native serialization.
        return new JacksonJsonMessageConverter();
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

}
