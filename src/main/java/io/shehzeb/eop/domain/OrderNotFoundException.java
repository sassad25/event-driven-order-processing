package io.shehzeb.eop.domain;

import java.util.UUID;

/** Raised when no event stream exists for the given order id. */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) {
        super("Order not found: " + id);
    }
}
