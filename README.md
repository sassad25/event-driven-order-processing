# Event-Driven Order Processing

An event-driven order system built with **Java 17**, **Spring Boot 3.3**, and **Apache Kafka**,
demonstrating senior-level distributed-systems patterns: **event sourcing**, **CQRS** (command/query
separation), **idempotent consumers**, and **dead-letter handling**.

> Author: **Muhammad Shehzeb Assad** — Senior Full Stack Java Developer
> Portfolio: https://github.com/sassad25

---

## What it demonstrates

| Concern                | Approach |
|------------------------|----------|
| **Event sourcing**     | Orders are stored as an append-only stream of immutable domain events; current state is rebuilt by folding the stream (`OrderAggregate.rehydrate`). |
| **CQRS**               | Commands append events and publish to Kafka; a consumer projects events into a denormalized `OrderSummary` read model optimized for queries. |
| **Ordering**           | Events are keyed by `orderId`, so all events for an order share a partition and preserve order. |
| **Idempotency**        | Each message carries a unique `messageId`; a `processed_messages` table makes consumption exactly-once in effect over Kafka's at-least-once delivery. |
| **Resilience / DLQ**   | A `DefaultErrorHandler` retries with back-off, then a `DeadLetterPublishingRecoverer` routes poison messages to `order-events.DLT` so the partition never blocks. |
| **Reliability**        | Idempotent producer (`enable.idempotence=true`, `acks=all`) and `read_committed` consumer isolation. |

## Flow

```
         POST /api/v1/orders
                │
                ▼
     ┌─────────────────────┐   append event    ┌────────────────┐
     │ OrderCommandService │ ────────────────▶  │  event_store   │  (append-only)
     └──────────┬──────────┘                    └────────────────┘
                │ publish (keyed by orderId)
                ▼
          ┌───────────┐   consume    ┌──────────────────────┐   project   ┌───────────────┐
          │   Kafka   │ ───────────▶ │  OrderEventConsumer   │ ──────────▶ │ order_summary │  (read model)
          │order-events│             │  (idempotent)         │             └───────────────┘
          └─────┬─────┘              └──────────┬────────────┘
                │                               │ on repeated failure
                │                               ▼
                │                      ┌──────────────────────┐
                └────────────────────▶ │  order-events.DLT    │  (dead-letter topic)
                                       └──────────────────────┘
```

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Kafka · Spring Data JPA · PostgreSQL / H2 · Jackson (polymorphic
event serialization) · Testcontainers (Kafka) · JUnit 5 · Awaitility · Docker · GitHub Actions.

## API

| Method | Path                              | Description                          |
|--------|-----------------------------------|--------------------------------------|
| POST   | `/api/v1/orders`                  | Place an order (returns 202 + id)    |
| POST   | `/api/v1/orders/{id}/payment`     | Mark order paid                      |
| POST   | `/api/v1/orders/{id}/cancellation`| Cancel order                         |
| GET    | `/api/v1/orders/{id}`             | Read projected order summary         |

Example:

```bash
curl -X POST http://localhost:8083/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-1","amount":250.00}'
# -> 202 { "orderId": "...", "status": "PLACED" }
```

## Running the tests

Integration tests spin up a real Kafka broker via Testcontainers, so **Docker must be running**:

```bash
mvn clean verify
```

Covers: event-sourced state reconstruction and the order state machine (unit), plus end-to-end
projection, status transitions, and idempotent duplicate handling over a live broker (integration).

## Running locally

```bash
docker compose up --build
```

Starts Kafka (KRaft mode), PostgreSQL, and the application on `http://localhost:8083`.

## Project layout

```
src/main/java/io/shehzeb/eop/
├── domain/        # Sealed OrderEvent hierarchy + event-sourced OrderAggregate
├── store/         # Append-only event store (JPA)
├── messaging/     # Kafka publisher, topics, error handling + DLQ config
├── consumer/      # Idempotent projector + processed-message tracking
├── readmodel/     # CQRS read model (OrderSummary)
├── service/       # Command service (append + publish)
└── api/           # REST controller, DTOs, RFC 7807 error handling
```

## License

MIT © Muhammad Shehzeb Assad
