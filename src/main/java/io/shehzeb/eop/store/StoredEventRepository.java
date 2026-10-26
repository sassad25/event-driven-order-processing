package io.shehzeb.eop.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Append-only event store access. Events are read back ordered by sequence number. */
public interface StoredEventRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByAggregateIdOrderBySequenceNumberAsc(UUID aggregateId);

    long countByAggregateId(UUID aggregateId);
}
