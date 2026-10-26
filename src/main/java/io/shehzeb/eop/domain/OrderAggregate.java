package io.shehzeb.eop.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order aggregate rebuilt by folding its event stream (event sourcing). Enforces the state
 * machine: an order may only be paid or cancelled while PLACED.
 */
public final class OrderAggregate {

    public enum Status { PLACED, PAID, CANCELLED }

    private final java.util.UUID id;
    private String customerId;
    private BigDecimal amount;
    private Status status;

    private OrderAggregate(java.util.UUID id) {
        this.id = id;
    }

    /** Replays events in order to reconstruct current state. */
    public static OrderAggregate rehydrate(java.util.UUID id, List<OrderEvent> events) {
        OrderAggregate agg = new OrderAggregate(id);
        events.forEach(agg::apply);
        return agg;
    }

    private void apply(OrderEvent event) {
        if (event instanceof OrderEvent.OrderPlaced e) {
            this.customerId = e.customerId();
            this.amount = e.amount();
            this.status = Status.PLACED;
        } else if (event instanceof OrderEvent.OrderPaid) {
            this.status = Status.PAID;
        } else if (event instanceof OrderEvent.OrderCancelled) {
            this.status = Status.CANCELLED;
        }
    }

    public void ensureCanTransition() {
        if (status != Status.PLACED) {
            throw new IllegalStateTransitionException(
                    "Order " + id + " is " + status + "; expected PLACED");
        }
    }

    public java.util.UUID id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Status status() {
        return status;
    }
}
