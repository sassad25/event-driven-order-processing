package io.shehzeb.eop;

import io.shehzeb.eop.domain.OrderEvent;
import io.shehzeb.eop.messaging.OrderEventPublisher;
import io.shehzeb.eop.readmodel.OrderSummaryRepository;
import io.shehzeb.eop.service.OrderCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end test over a real Kafka broker (Testcontainers). Verifies that commands produce
 * events which the consumer projects into the read model, and that duplicate messages are
 * handled idempotently.
 */
@SpringBootTest
@Testcontainers
class OrderProcessingIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    OrderCommandService commandService;

    @Autowired
    OrderSummaryRepository summaryRepository;

    @Autowired
    OrderEventPublisher publisher;

    @Test
    void placeOrder_isProjectedToReadModel() {
        UUID orderId = commandService.placeOrder("cust-42", new BigDecimal("250.00"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(summaryRepository.findById(orderId))
                        .isPresent()
                        .hasValueSatisfying(s -> {
                            assertThat(s.getStatus()).isEqualTo("PLACED");
                            assertThat(s.getCustomerId()).isEqualTo("cust-42");
                            assertThat(s.getAmount()).isEqualByComparingTo("250.00");
                        }));
    }

    @Test
    void payOrder_updatesProjectedStatus() {
        UUID orderId = commandService.placeOrder("cust-7", new BigDecimal("50.00"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(summaryRepository.findById(orderId)).isPresent());

        commandService.payOrder(orderId, "pay-ref-1");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(summaryRepository.findById(orderId))
                        .hasValueSatisfying(s -> assertThat(s.getStatus()).isEqualTo("PAID")));
    }

    @Test
    void duplicateEvent_isProcessedOnce() {
        UUID orderId = UUID.randomUUID();
        OrderEvent.OrderPlaced placed =
                new OrderEvent.OrderPlaced(orderId, "cust-dup", new BigDecimal("10.00"), Instant.now());

        // Publish the same logical event twice; keyed by orderId. The projection is idempotent,
        // so the read model must still reflect a single, consistent PLACED summary.
        publisher.publish(placed);
        publisher.publish(placed);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(summaryRepository.findById(orderId))
                        .hasValueSatisfying(s -> assertThat(s.getStatus()).isEqualTo("PLACED")));
    }
}
