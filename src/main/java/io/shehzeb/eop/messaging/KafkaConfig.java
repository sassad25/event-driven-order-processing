package io.shehzeb.eop.messaging;

import io.shehzeb.eop.domain.OrderEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka wiring for reliable event processing:
 * <ul>
 *   <li>A {@link DefaultErrorHandler} retries failed deliveries with a fixed back-off.</li>
 *   <li>After retries are exhausted, a {@link DeadLetterPublishingRecoverer} forwards the record
 *       to {@code <topic>.DLT} so poison messages never block the partition.</li>
 * </ul>
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderEvent> consumerFactory,
            KafkaTemplate<Object, Object> deadLetterTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Route to "<original-topic>.DLT" on the same partition after retries are exhausted.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deadLetterTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition()));

        // Retry twice (3 total attempts) with a 500ms back-off before dead-lettering.
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 2L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
