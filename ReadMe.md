# TicketDispatcherService - **Asynchronous Ticketing System**
Demo project showcasing the capabilities of Gentian Licenji as a Senior Software Engineer.
<br>This project is a simplified backend ticketing system built with **Spring WebFlux** and **Apache Kafka**, demonstrating an **asynchronous, event-driven architecture**. 
<br>enables users to submit support tickets via a REST API, which are then processed and managed through Kafka topics and reactive services.

#### 🔧 Tech Stack

* **Java + Spring Boot (WebFlux)** – reactive, non-blocking web stack
* **Apache Kafka** – asynchronous event streaming
* **R2DBC** – reactive, non-blocking database connectivity (replaces traditional JDBC)
* **Reactive Programming** – powered by Project Reactor
* **Pluggable idempotency layer** – in-memory Map (dev) → Redis (production)
* **JWT authentication** – secret externalized via environment config

#### 🚀 Key API Features
[View OpenAPI spec](docs/TicketDispatcher-v1.0.yaml)

#### 🗝️ Key components:
- 🔍 `filter`: Request/Response processing
- 🎮 `controller`: API endpoints
- 🎯 `delegate`: Business logic delegation
- ⚙️ `service`: Business logic
- 💾 `repository`: Data access
- ⚠️ `exception`: Error handling
- 🔒 `config`: Application configurations
- 📝 `model`: Data structures
- 🛠️ `util`: Helper classes

This setup reflects modern backend engineering best practices, including **decoupling**, **non-blocking I/O**, and **message-driven communication**.

------

# [Design Decisions](Design.md)
Please focus on this document as it is important to me. 
<br>It is the essence of why we do what we do.

------

## Kafka Message Format & Reactive Integration

### Serialization

* **Key Serializer**: `KafkaAvroSerializer`
* **Value Serializer**: `KafkaAvroSerializer`
* **Key Deserializer**: `KafkaAvroDeserializer`
* **Value Deserializer**: `KafkaAvroDeserializer`
* **Schema Registry**: `spring.kafka.schema‑registry.url` → Confluent Schema Registry
* **UUID Logical Type**: binary (`bytes`), 16 bytes per UUID

### Topic & Schema Registration

Defined in `KafkaTopicConfig`:

```java
@Bean NewTopic ticketCreateTopic() { … .partitions(12).replicas(1).config(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4") … }
@PostConstruct registerSchemas() { client.register("ticket-create.v1-value", TicketCreated.SCHEMA$); … }
```

| Event               | Topic                   | Avro Class            |
| ------------------- | ----------------------- | --------------------- |
| TicketCreated       | `ticket-create.v1`      | `TicketCreated`       |
| TicketAssigned      | `ticket-assignments.v1` | `TicketAssigned`      |
| TicketStatusUpdated | `ticket-updates.v1`     | `TicketStatusUpdated` |

Sure — here’s a concise, professional version for your README:

### 📄 TicketCreated Avro Schema

Represents a newly created ticket event.

* **Namespace:** `com.pleased.ticket.dispatcher.server.model.events`
* **Type:** `record`
* **Name:** `TicketCreated`

| Field         | Type                      | Notes                    |
| ------------- | ------------------------- | ------------------------ |
| `eventId`     | `bytes` (uuid)            | Event UUID               |
| `ticketId`    | `bytes` (uuid)            | Ticket UUID              |
| `subject`     | `string`                  | Ticket title             |
| `description` | `["null", "string"]`      | Optional description     |
| `userId`      | `bytes` (uuid)            | Creator's UUID           |
| `projectId`   | `bytes` (uuid)            | Project UUID             |
| `createdAt`   | `long` (timestamp-millis) | Creation timestamp (UTC) |

> UUIDs use Avro `bytes` with logicalType `uuid`.
> Timestamps use `timestamp-millis` for millisecond precision.

### Producer: `KafkaProducerConfig` & `TicketEventProducer`

* **Reactive Template**: `ReactiveKafkaProducerTemplate<ByteBuffer, Object>`
* **Idempotence**: `ENABLE_IDEMPOTENCE=true`, `ACKS=all`
* **Compression**: `snappy`
* **Retry**: 3× with 100 ms backoff
* **Headers**:

   * `eventType`: set from `KafkaTopicConfig.EVENT_TYPE_MAP`
   * correlation ID: encoded via `UUIDConverter.uuidToBytes(correlationId)` and passed as `SenderRecord.correlationMetadata()`
* **Metrics**: `Micrometer` timers & counters around `.send(...)`

```java
private Mono<Void> publishEvent(...) {
  SenderRecord<ByteBuffer,Object,ByteBuffer> record = SenderRecord.create(
    topic, null, null, key, event, uuidToBytes(correlationId)
  );
  record.headers().add("eventType", topicToType.getBytes());
  return reactiveKafkaTemplate.send(record)…
}
```

### Consumer: `KafkaConsumerConfig` & `TicketEventConsumer`

* **Reactive Template**: `ReactiveKafkaConsumerTemplate<ByteBuffer, T>`
* **Deserializers**: Avro-specific reader enabled (`SPECIFIC_AVRO_READER_CONFIG=true`)
* **Auto‑commit**: Disabled; uses `.receiveAutoAck()` + manual commit batches
* **Concurrency**: Controlled in the reactive pipeline via `.flatMap(...)`
* **Error Handling**: `DefaultErrorHandler` with a `FixedBackOff(1 s, 3)` → TODO: DLQ
* **Group IDs**: one per topic (e.g. `ticket-service-create-consumer-reactive`)
* **Processing**:

   * `receiveAutoAck()` → `.flatMap(this::handleXxx)` → `.retry(3)`
   * Each handler maps Avro record → domain entity → `ticketRepository.save(...)`

```java
reactiveTicketCreatedConsumer.receiveAutoAck()
  .doOnNext(r -> log.info("Processing: {}", r.value()))
  .flatMap(this::handleTicketCreated)
  .retry(3)
  .subscribe();
```

### Key Strategy & Ordering

* **Partition Key**: binary `ticketId` ensures all events for a ticket land in the same partition
* **In‑order Guarantees**: leveraging partition affinity + reactive back‑pressure

### Observability & Future Enhancements

* **Metrics**: producer send durations, error counters
* **Structured Logging**: include `topic`, `partition`, `offset`, `eventType`, `correlationId`
* **To‑do**:

   * Dead‑letter topics
   * Schema compatibility strategy
   * Move common metadata (`eventId`, `eventVersion`) into Kafka headers

---

## 🛠️ Setup & Run Instructions

This is an asynchronous ticketing system built with **Java 8**, **Spring Boot 2.7.18**, **WebFlux**, and **Kafka**. It uses **Netty** as the embedded server and an **H2 in-memory database**. The REST API is exposed at:

```
http://localhost:8888
```

### ✅ Prerequisites

* Java 8
* Maven 3.6+
* Docker & Docker Compose (for Kafka)
* Postman or Newman CLI (for API testing)
* Apache JMeter (for load testing)

### 📦 Build the Application

```bash
mvn clean package
```

This will generate the JAR:

```
target/TicketDispatcherServer.jar
```

### ▶️ Run the Application

#### Option 1: Default Configuration (uses internal `application.properties`)

```bash
mvn spring-boot:run
```

or

```bash
java -jar target/TicketDispatcherServer.jar
```

#### Option 2: Custom Configuration (external `application.properties`)

To run the application using an external config file:

```bash
java -jar target/TicketDispatcherServer.jar \
  --spring.config.location=file:/path/to/custom/application.properties
```
Got it! Here’s a concise **Option 3** for Docker:

#### Option 3: Using Docker

Build and start the app with Docker:

```bash
docker build -t ticket-dispatcher-service:2.0 .
docker compose up -d
```

Stop the app:

```bash
docker compose down -v
```
Link to detailed readme guide [LoadTestingGuide.md](LoadTestingGuide.md).

### 🐳 Start Kafka Using Docker Compose
Link to detailed readme guide [LoadTestingGuide.md](LoadTestingGuide.md).

## 🧪 API Testing with Postman

### Option 1: Postman GUI

1. Open Postman
2. Import the collection file:

   ```
   /docs/Postman-Test-Suite-TicketDispatcher-v1.0.json
   ```
3. Run the test collection manually via the Runner tab

### Option 2: Newman CLI

If you have [Newman](https://www.npmjs.com/package/newman) installed:

```bash
newman run docs/Postman-Test-Suite-TicketDispatcher-v1.0.json
```

Optional: run with an environment file

```bash
newman run docs/Postman-Test-Suite-TicketDispatcher-v1.0.json \
  -e docs/local.postman_environment.json
```

## ⚙️ Load Testing with JMeter
Link to detailed readme guide [LoadTestingGuide.md](LoadTestingGuide.md).

### 🔍 No access H2 Console

The H2 Console is incompatible with Spring WebFlux because it requires a blocking JDBC connection, 
<br>whereas WebFlux applications use non-blocking R2DBC drivers that operate on an entirely separate reactive stack.
<br>**Note:** It is possible only if you're using a live (persistent) H2 instance—such as jdbc:h2:file: or jdbc:h2:tcp:
---------

## 🧪 Tests Included

This module includes integration and component-level tests under [`/src/test/java`](src/test/java), focused on validating WebFlux reactive APIs, Kafka event flow, and system boundaries.

### ✅ End-to-End

* [`TicketsAPIE2ETest`](src/test/java/com/pleased/ticket/dispatcher/server/TicketsAPIE2ETest.java): Simulates full API behavior using `WebTestClient`. Includes a custom Kafka consumer to validate emitted events for correctness.

### 🌐 Controller Layer
These are slice integration tests that load a partial Spring context, focused on controller behavior (with filters, core services and delegates), but excluding services like Kafka, databases, etc.

* [`TicketsControllerPositiveIT`](src/test/java/com/pleased/ticket/dispatcher/server/controller/TicketsControllerPositiveIT.java): Uses `WebTestClient` to test API success responses.
* [`TicketsControllerNegativeIT`](src/test/java/com/pleased/ticket/dispatcher/server/controller/TicketsControllerNegativeIT.java): Uses `WebTestClient` to test API failure responses.
* [`TicketsControllerAuthIT`](src/test/java/com/pleased/ticket/dispatcher/server/controller/TicketsControllerAuthIT.java): Covers authentication and authorization scenarios.

### 🧪 Service Layer

* [`TicketsApiServiceTest`](src/test/java/com/pleased/ticket/dispatcher/server/service/TicketsApiServiceTest.java): Mocks all external dependencies to verify business logic and method invocation paths.

### 🔁 Kafka Integration

* [`TicketEventConsumerIT.java`](src/test/java/com/pleased/ticket/dispatcher/server/service/TicketEventConsumerIT.java): Creates mock user and project records, consumes Kafka events, and asserts correct DB inserts.
* [`TicketEventProducerIT`](src/test/java/com/pleased/ticket/dispatcher/server/service/TicketEventProducerIT.java): Validates that `TicketCreate` events are properly published by setting up a Kafka listener.

### ⚙️ Configuration

* [`DisableSecurityConfig`](src/test/java/com/pleased/ticket/dispatcher/server/config/DisableSecurityConfig.java), [`TestKafkaConfig`](src/test/java/com/pleased/ticket/dispatcher/server/config/TestKafkaConfig.java): Utility configurations for disabling security and setting up embedded Kafka during test execution.

> Note: Tests suffixed with `IT` indicate integration-level coverage.

---

#### 🤖 AI-Assisted Development

This project leverages AI tools to accelerate development while maintaining code quality and engineering rigor:

**🔍 Validation Process:**
* **Critical thinking** of all AI-generated code and architectural decisions
* **Best practices verification** against official documentation, community-vetted examples, and known design principles.
* **Peer review mindset** - treating AI as a junior developer requiring oversight.
* **Continuous learning** AI output often sparked further investigation into edge cases or deeper understanding of frameworks.

**🎯 AI Usage Areas:**
* Boilerplate code generation and configuration setup
* Documentation drafting and technical writing assistance
* Code review suggestions and optimization recommendations
* Architecture pattern validation and implementation guidance

> **Engineering Principle**: The tools we use are only as good as we are.