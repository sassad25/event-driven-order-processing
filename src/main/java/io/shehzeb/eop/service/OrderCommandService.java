package io.shehzeb.eop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shehzeb.eop.domain.OrderAggregate;
import io.shehzeb.eop.domain.OrderEvent;
import io.shehzeb.eop.domain.OrderNotFoundException;
import io.shehzeb.eop.messaging.OrderEventPublisher;
import io.shehzeb.eop.store.StoredEvent;
import io.shehzeb.eop.store.StoredEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Command side of the system. Each command:
 * <ol>
 *   <li>rehydrates the aggregate from its event stream,</li>
 *   <li>validates the state transition,</li>
 *   <li>appends a new event to the append-only store (transactional), and</li>
 *   <li>publishes the event to Kafka.</li>
 * </ol>
 */
@Service
public class OrderCommandService {

    private final StoredEventRepository eventRepository;
    private final OrderEventPublisher publisher;
    private final ObjectMapper objectMapper;

    public OrderCommandService(StoredEventRepository eventRepository,
                               OrderEventPublisher publisher,
                               ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID placeOrder(String customerId, BigDecimal amount) {
        UUID orderId = UUID.randomUUID();
        OrderEvent.OrderPlaced event = new OrderEvent.OrderPlaced(orderId, customerId, amount, Instant.now());
        appendAndPublish(orderId, event);
        return orderId;
    }

    @Transactional
    public void payOrder(UUID orderId, String paymentReference) {
        OrderAggregate aggregate = load(orderId);
        aggregate.ensureCanTransition();
        appendAndPublish(orderId, new OrderEvent.OrderPaid(orderId, paymentReference, Instant.now()));
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        OrderAggregate aggregate = load(orderId);
        aggregate.ensureCanTransition();
        appendAndPublish(orderId, new OrderEvent.OrderCancelled(orderId, reason, Instant.now()));
    }

    @Transactional(readOnly = true)
    public OrderAggregate load(UUID orderId) {
        List<StoredEvent> stored = eventRepository.findByAggregateIdOrderBySequenceNumberAsc(orderId);
        if (stored.isEmpty()) {
            throw new OrderNotFoundException(orderId);
        }
        List<OrderEvent> events = stored.stream().map(this::deserialize).toList();
        return OrderAggregate.rehydrate(orderId, events);
    }

    private void appendAndPublish(UUID orderId, OrderEvent event) {
        long nextSequence = eventRepository.countByAggregateId(orderId);
        eventRepository.save(new StoredEvent(orderId, nextSequence,
                event.getClass().getSimpleName(), serialize(event)));
        publisher.publish(event);
    }

    private String serialize(OrderEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }

    private OrderEvent deserialize(StoredEvent stored) {
        try {
            return objectMapper.readValue(stored.getPayload(), OrderEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize event", e);
        }
    }
}
