package io.shehzeb.eop.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only persistence record for a single domain event. The event payload is stored as JSON.
 * A per-aggregate monotonic {@code sequenceNumber} preserves ordering and enables optimistic
 * concurrency on the stream.
 */
@Entity
@Table(name = "event_store", indexes = {
        @Index(name = "idx_event_aggregate", columnList = "aggregateId, sequenceNumber")
})
public class StoredEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private long sequenceNumber;

    @Column(nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant storedAt;

    protected StoredEvent() {
    }

    public StoredEvent(UUID aggregateId, long sequenceNumber, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.payload = payload;
        this.storedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getStoredAt() {
        return storedAt;
    }
}
