package io.shehzeb.eop.consumer;

import org.springframework.data.jpa.repository.JpaRepository;

/** Tracks handled message ids for idempotent consumption. */
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
}
