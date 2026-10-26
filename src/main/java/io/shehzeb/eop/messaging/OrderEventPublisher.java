package io.shehzeb.eop.messaging;

import io.shehzeb.eop.domain.OrderEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes order events to Kafka. Messages are keyed by order id so that all events for a given
 * order land on the same partition and preserve per-order ordering. A unique {@code messageId}
 * header enables idempotent consumption downstream.
 */
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderEvent event) {
        Message<OrderEvent> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, Topics.ORDER_EVENTS)
                .setHeader(KafkaHeaders.KEY, event.orderId().toString())
                .setHeader("messageId", UUID.randomUUID().toString())
                .build();
        kafkaTemplate.send(message);
    }
}
