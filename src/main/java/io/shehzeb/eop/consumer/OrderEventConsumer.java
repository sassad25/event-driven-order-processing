package io.shehzeb.eop.consumer;

import io.shehzeb.eop.domain.OrderEvent;
import io.shehzeb.eop.messaging.Topics;
import io.shehzeb.eop.readmodel.OrderSummary;
import io.shehzeb.eop.readmodel.OrderSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects order events into the {@link OrderSummary} read model.
 *
 * <p><strong>Idempotency:</strong> each message carries a unique {@code messageId}; the consumer
 * records handled ids and skips duplicates, giving exactly-once effect over Kafka's at-least-once
 * delivery.</p>
 *
 * <p><strong>Error handling:</strong> unrecoverable failures propagate so the container's
 * {@code DefaultErrorHandler} retries and then routes the message to the dead-letter topic.</p>
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderSummaryRepository summaryRepository;
    private final ProcessedMessageRepository processedRepository;

    public OrderEventConsumer(OrderSummaryRepository summaryRepository,
                              ProcessedMessageRepository processedRepository) {
        this.summaryRepository = summaryRepository;
        this.processedRepository = processedRepository;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "order-projector")
    @Transactional
    public void onEvent(@Payload OrderEvent event,
                        @Header(name = "messageId", required = false) String messageId) {

        if (messageId != null && processedRepository.existsById(messageId)) {
            log.debug("Skipping already-processed message {}", messageId);
            return;
        }

        project(event);

        if (messageId != null) {
            try {
                processedRepository.save(new ProcessedMessage(messageId));
            } catch (DataIntegrityViolationException race) {
                // Concurrent delivery of the same id — safe to ignore, projection is idempotent.
                log.debug("Duplicate messageId {} on concurrent delivery", messageId);
            }
        }
    }

    private void project(OrderEvent event) {
        if (event instanceof OrderEvent.OrderPlaced e) {
            summaryRepository.save(new OrderSummary(e.orderId(), e.customerId(), e.amount(), "PLACED"));
        } else if (event instanceof OrderEvent.OrderPaid e) {
            updateStatus(e.orderId(), "PAID");
        } else if (event instanceof OrderEvent.OrderCancelled e) {
            updateStatus(e.orderId(), "CANCELLED");
        }
    }

    private void updateStatus(java.util.UUID orderId, String status) {
        summaryRepository.findById(orderId).ifPresent(summary -> {
            summary.updateStatus(status);
            summaryRepository.save(summary);
        });
    }
}
