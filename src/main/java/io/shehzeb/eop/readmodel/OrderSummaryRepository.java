package io.shehzeb.eop.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Query-side repository over the projected order summaries. */
public interface OrderSummaryRepository extends JpaRepository<OrderSummary, UUID> {
}
