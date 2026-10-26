package io.shehzeb.eop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Event-Driven Order Processing.
 *
 * <p>Orders are accepted via REST, persisted as an event-sourced stream of domain events,
 * and published to Kafka. A consumer projects those events into a read model and handles
 * downstream processing idempotently, with failed messages routed to a dead-letter topic.</p>
 */
@SpringBootApplication
public class OrderProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderProcessingApplication.class, args);
    }
}
