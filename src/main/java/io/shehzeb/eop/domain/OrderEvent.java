package io.shehzeb.eop.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Sealed hierarchy of order domain events. Events are immutable facts; the current state of an
 * order is derived by folding its event stream. Jackson polymorphic typing serializes the concrete
 * type so events round-trip through Kafka and the event store.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderEvent.OrderPlaced.class, name = "OrderPlaced"),
        @JsonSubTypes.Type(value = OrderEvent.OrderPaid.class, name = "OrderPaid"),
        @JsonSubTypes.Type(value = OrderEvent.OrderCancelled.class, name = "OrderCancelled")
})
public sealed interface OrderEvent permits
        OrderEvent.OrderPlaced, OrderEvent.OrderPaid, OrderEvent.OrderCancelled {

    UUID orderId();

    Instant occurredAt();

    record OrderPlaced(UUID orderId, String customerId, BigDecimal amount, Instant occurredAt)
            implements OrderEvent {
    }

    record OrderPaid(UUID orderId, String paymentReference, Instant occurredAt)
            implements OrderEvent {
    }

    record OrderCancelled(UUID orderId, String reason, Instant occurredAt)
            implements OrderEvent {
    }
}
