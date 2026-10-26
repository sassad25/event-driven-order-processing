package io.shehzeb.eop.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests for event-sourced state reconstruction and the order state machine. */
class OrderAggregateTest {

    private final UUID id = UUID.randomUUID();

    @Test
    void rehydrate_appliesEventsInOrder() {
        OrderAggregate agg = OrderAggregate.rehydrate(id, List.of(
                new OrderEvent.OrderPlaced(id, "cust-1", new BigDecimal("99.00"), Instant.now()),
                new OrderEvent.OrderPaid(id, "pay-123", Instant.now())
        ));

        assertThat(agg.status()).isEqualTo(OrderAggregate.Status.PAID);
        assertThat(agg.customerId()).isEqualTo("cust-1");
        assertThat(agg.amount()).isEqualByComparingTo("99.00");
    }

    @Test
    void ensureCanTransition_failsWhenNotPlaced() {
        OrderAggregate agg = OrderAggregate.rehydrate(id, List.of(
                new OrderEvent.OrderPlaced(id, "cust-1", new BigDecimal("10.00"), Instant.now()),
                new OrderEvent.OrderCancelled(id, "changed mind", Instant.now())
        ));

        assertThatThrownBy(agg::ensureCanTransition)
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void ensureCanTransition_succeedsWhenPlaced() {
        OrderAggregate agg = OrderAggregate.rehydrate(id, List.of(
                new OrderEvent.OrderPlaced(id, "cust-1", new BigDecimal("10.00"), Instant.now())
        ));

        agg.ensureCanTransition(); // should not throw
        assertThat(agg.status()).isEqualTo(OrderAggregate.Status.PLACED);
    }
}
