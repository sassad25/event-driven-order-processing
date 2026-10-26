package io.shehzeb.eop.messaging;

/** Topic names used across producer, consumer and dead-letter handling. */
public final class Topics {

    private Topics() {
    }

    public static final String ORDER_EVENTS = "order-events";
    public static final String ORDER_EVENTS_DLT = "order-events.DLT";
}
