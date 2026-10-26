package io.shehzeb.eop.domain;

/** Raised when an order command is invalid for the aggregate's current state. */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
