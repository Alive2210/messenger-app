package com.messenger.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.messages:messages.queue}")
    private String messagesQueue;

    @Value("${rabbitmq.queue.files:files.queue}")
    private String filesQueue;

    @Value("${rabbitmq.queue.notifications:notifications.queue}")
    private String notificationsQueue;

    @Value("${rabbitmq.exchange:messenger.exchange}")
    private String exchange;

    @Bean
    public Queue messagesQueue() {
        return QueueBuilder.durable(messagesQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", messagesQueue + ".dlq")
                .build();
    }

    @Bean
    public Queue messagesDLQ() {
        return QueueBuilder.durable(messagesQueue + ".dlq").build();
    }

    @Bean
    public Queue filesQueue() {
        return QueueBuilder.durable(filesQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", filesQueue + ".dlq")
                .build();
    }

    @Bean
    public Queue filesDLQ() {
        return QueueBuilder.durable(filesQueue + ".dlq").build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(notificationsQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", notificationsQueue + ".dlq")
                .build();
    }

    @Bean
    public Queue notificationsDLQ() {
        return QueueBuilder.durable(notificationsQueue + ".dlq").build();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding messagesBinding(Queue messagesQueue, TopicExchange exchange) {
        return BindingBuilder.bind(messagesQueue).to(exchange).with("message.#");
    }

    @Bean
    public Binding filesBinding(Queue filesQueue, TopicExchange exchange) {
        return BindingBuilder.bind(filesQueue).to(exchange).with("file.#");
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationsQueue).to(exchange).with("notification.#");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
