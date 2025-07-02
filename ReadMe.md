# TicketDispatcherService - **Asynchronous Ticketing System**
Demo project showcasing the capabilities of Gentian Licenji as a Senior Software Engineer.
<br>This project is a simplified backend ticketing system built with **Spring WebFlux** and **Apache Kafka**, demonstrating an **asynchronous, event-driven architecture**. 
<br>enables users to submit support tickets via a REST API, which are then processed and managed through Kafka topics and reactive services.

#### 🔧 Tech Stack

* **Java + Spring Boot (WebFlux)**
* **Apache Kafka** for asynchronous messaging
* **Pluggable idempotency layer** - in-memory Map (dev) → Redis (production)
* **Reactive Programming** using Project Reactor
* **JWT authentication** - to be externalized secret via environment configuration

#### 🚀 Key API Features
[View OpenAPI spec](docs/TicketDispatcher-v1.0.yaml)

#### 🗝️ Key components:
- 🔍 `filter`: Request/Response processing
- 🎮 `controller`: API endpoints
- 🎯 `delegate`: Business logic delegation
- ⚙️ `service`: Business logic
- 🔒 `config`: Application configurations
- ⚠️ `exception`: Error handling
- 📝 `model`: Data structures
- 💾 `repository`: Data access
- 🛠️ `util`: Helper classes

This setup reflects modern backend engineering best practices, including **decoupling**, **non-blocking I/O**, and **message-driven communication**.

------

# [Design Decisions](Design.md)
Please focus on this document as it is important to me. 
<br>It is the essence of why we do what we do.

------

# Kafka Message Format Guide

## Message Serialization
- **Format**: JSON serialization using Spring Kafka's `JsonSerializer`/`JsonDeserializer`
- **Key**: String (UUID converted to string)
- **Value**: JSON-serialized event objects

#### 📂 Event Types

* `TicketCreated`
* `TicketAssigned`
* `TicketStatusUpdated`

## Event Structure

All events inherit from a base `TicketEvent` class with common fields:

```json
{
  "eventId": "uuid",
  "correlationId": "uuid", 
  "eventVersion": 1
}
```

### TicketCreated Event
**Topic**: `ticket-create.v1`
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001", 
  "eventVersion": 1,
  "ticketId": "550e8400-e29b-41d4-a716-446655440002",
  "subject": "Bug in user authentication",
  "description": "Users cannot log in with valid credentials",
  "userId": "550e8400-e29b-41d4-a716-446655440003",
  "projectId": "550e8400-e29b-41d4-a716-446655440004",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

## Topics Configuration
- **Partitions**: 12 per topic (for high throughput)
- **Compression**: LZ4
- **Replication**: 1 (single broker setup)
- **Segment time**: 10 minutes

## Message Key Strategy
- All messages use `ticketId` as the partition key
- Idempotency keys are required by the client on the API level and are stored as `eventId`
- Idempotency is enforced by the [IdempotencyFilter.java](src/main/java/com/pleased/ticket/dispatcher/server/filter/IdempotencyFilter.java).
- Ensures related ticket events are processed in order within the same partition

## Consumer Configuration
- **Group ID**: `ticket-dispatcher-service`
- **Isolation Level**: `read_committed` (transactional support)
- **Auto Commit**: Disabled (manual acknowledgment)
- **Trusted Packages**: `*` (accepts all JSON types)

## Error Handling
- **Retry Policy**: 3 attempts with 1-second fixed backoff
- **Transaction Support**: Enabled with `KafkaTransactionManager`

## Potential Improvements - *Incomplete-TODO*

### Message Format Optimizations
- **UUID Serialization**: Consider binary UUID serialization instead of string conversion to reduce message size by ~50% (16 bytes vs 36 bytes per UUID)
- **Schema Registry**: Implement Avro/Protobuf with Confluent Schema Registry for better schema evolution, type safety, and smaller payload sizes
- **Message Headers**: Move metadata (`eventId`, `correlationId`, `eventVersion`) to Kafka headers to separate business data from infrastructure concerns

### Performance Enhancements
- **Custom Partitioning**: Implement custom partitioner based on `projectId` + `ticketId` hash for better load distribution across related entities
- **Batch Processing**: Setup consumers to do batch DB inserts and event processing for higher throughput
- **Compression**: Evaluate zstd compression for better compression ratios than LZ4, especially for JSON payloads

### Reliability & Monitoring
- **Dead Letter Topics**: Add DLT configuration for failed message handling
- **Message Timestamps**: Include `publishedAt` timestamp for better observability and message age tracking

---

## 🛠️ Setup & Run Instructions

This is an asynchronous ticketing system built with **Java 8**, **Spring Boot 2.7.18**, **WebFlux**, and **Kafka**. It uses **Netty** as the embedded server and an **H2 in-memory database**. The REST API is exposed at:

```
http://localhost:8080
```

### ✅ Prerequisites

* Java 8
* Maven 3.6+
* Docker & Docker Compose (for Kafka)
* Postman or Newman CLI (for API testing)
* Apache JMeter (for load testing)

### 📦 Build the Application

```bash
mvn clean install
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

### 🔍 Access H2 Console

To view the in-memory H2 database:

* URL: `http://localhost:8080/h2-console`
* JDBC URL: `jdbc:h2:mem:testdb`
* User: `sa`
* Password: *(leave blank)*


---------

## 🧪 Tests Included

This module includes integration and component-level tests under [`/src/test/java`](src/test/java), focused on validating WebFlux reactive APIs, Kafka event flow, and system boundaries.

### ✅ End-to-End

* [`TicketsAPIE2ETest`](src/test/java/com/pleased/ticket/dispatcher/server/TicketsAPIE2ETest.java): Simulates full API behavior using `WebTestClient`. Includes a custom Kafka consumer to validate emitted events for correctness.

### 🌐 Controller Layer

* [`TicketsControllerIT`](src/test/java/com/pleased/ticket/dispatcher/server/controller/TicketsControllerIT.java): Uses `WebTestClient` to test API success/failure responses and Kafka event side-effects.
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