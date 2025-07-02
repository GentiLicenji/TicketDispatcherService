# Design and Implementation consideration

## 0. Ticketing System Task breakdown
The following is an overview of all the steps taken in order to complete the task assigned.

1. Research:
   - Gather non-functional requirements
   - Design high-level system architecture
   - API and data model design
   - Derive design decision and impacts for the task.

2. Application development:
   - Generate project scaffolding [(Spring Initializr)](https://start.spring.io/)
   - Create API contract [(OpenAPI Spec)](https://swagger.io/specification/)
   - Generate REST controllers and models [(Swagger Codegen)](https://swagger.io/tools/swagger-codegen/)
   - Implement cross-cutting concerns (exception handling,security,logging filter,audit etc.) [(Claude 4.0)](https://www.anthropic.com/news/claude-4)
   - Refactor code:
       - separate layers
       - apply best practices
       - fix/debug issues
   - Create custom metrics

3. Testing/tuning phase:
   - Apply performance tuning
   - Create integration test for business logic validation.
   - Create load tests to tweak and validate performance metrics.
   - Create mock tests for failure scenarios.

4. Cloud DevOps:
   - Create multi-stage dockerfile:
     - Tune alpine base image
     - Apply JVM tuning
     - Enforce runtime security constraints
   - Create docker compose.yaml ( K8S is superior for container orchestration )

**Breakdown** into sub-tickets:
- PLS-1 : Research
- PLS-2 : Application development
- PLS-3 : Testing/tuning phase
- PLS-4 : Cloud DevOps 

-----

## 2.1 Generated project scaffolding
We started here in order to have a repo and have a folder structure to start with.

<p align="left">
  <img src="docs/initial-setup.png" width="600" />
</p>

-----

## 1.0 Research

### 1.1 Gather non-functional requirements
For this section I used ChatGPT's deep research mode to save time digging for real numbers on current stack and traffic loads.

#### Current stack
Analysis of Pleased.com’s public site and job postings reveals a modern cloud-native SaaS stack. 
The **frontend** is built with Vue.js (with Vuex, Vuetify, TypeScript) and even uses Firebase for real-time features. The **backend** is implemented in Java (Spring Boot/Spring Data) on AWS, using Docker containers orchestrated by Kubernetes. Data storage uses a relational SQL database (MySQL), with Redis for caching and RabbitMQ (or similar MQ) for asynchronous messaging. CI/CD is handled via Jenkins, and static assets (HTML/CSS/JS/images) are likely served through a CDN (e.g. AWS CloudFront) for low latency.

* Key inferred components include:
* **Frontend:** Vue.js (Vuex, Vuetify), SCSS, Webpack, Firebase Realtime Database.
* **Backend:** Java 8+ (Spring Boot/Security), RESTful APIs, Microservices architecture.
* **Database:** MySQL (primary), possibly read-replicas or sharding for scale; Redis for in-memory caching.
* **DevOps/Infra:** AWS Cloud (EC2, EKS/ECS), Docker & Kubernetes for containers; Jenkins pipelines; CloudWatch/Prometheus for monitoring.
* **CDN/Caching:** Likely a CDN (CloudFront or similar) for static content; use of HTTP caching headers and Redis/CloudCache to speed up API responses.
* **Auxiliary:** Service registry/discovery (Kubernetes DNS or Consul) and API gateway (e.g. Envoy/Nginx) for routing. (Slack, a similar SaaS, uses Envoy and siloed “cells” to route traffic.)

Collectively, these point to a microservices-based design on AWS, optimized for scalability and agility. Indeed, Pleased’s job descriptions explicitly mention Docker/K8s microservices to handle “hundreds of thousands of customers a day”, mirroring architectures used by large SaaS like Zendesk (which moved from a Rails monolith to Kubernetes).

#### Traffic and Load Estimates
Pleased targets **high volume** but smaller scale than global giants. Job postings claim “hundred and thousands of customers a day” (interpreted as 10^5–10^6 daily users). For context, large SaaS platforms see enormous loads (Atlassian’s Tenant Context Service handled \~32 billion requests/day, peaking \~586k requests/sec; Slack supports millions of concurrent chat events). We should assume Pleased’s usage pattern is **spiky** (peaks during business hours) with possibly thousands of requests per second at load peaks.

Recommendations for handling this include: use AWS **Auto Scaling** for compute (EC2/ECS/EKS) so instances scale with load; deploy services across multiple Availability Zones for fault tolerance; and employ **load balancers** (ALB/NLB) and DNS-based traffic distribution. Caching layers (Redis caches, CDN edge caches) will reduce DB/API load. Queue systems (RabbitMQ/Kafka) can smooth bursts by decoupling writes. These patterns are common in high-traffic SaaS: e.g. Slack’s cell-based AWS architecture isolates AZ failures, allowing quick traffic shifts away from failing zones.

> **Usage Analogy Table (Examples):**

> | Platform             | Scale (daily)                | Notes/Tech                                         |
> | -------------------- | ---------------------------- | -------------------------------------------------- |
> | Pleased.com (est.)   | \~10^5–10^6 users/day        | Java/Spring, MySQL, AWS/K8s; thousands req/s peak. |
> | Slack (chat SaaS)    | Millions of users (events)   | AWS region-based cells, Envoy routers, EKS.        |
> | Atlassian TCS (META) | \~32×10^9 req/day (586k/sec) | DynamoDB + in-memory cache + SNS invalidation.     |

### 1.2 Design high-level system architecture (L3)
Using the data above we can create some rough estimates on the non-functional requirements.
<br>Since this is a day-to-day business tool than the active daily users will match to the total nr of users.
<br>Current peak traffic can be estimated in the thousands.
<br>Due to the type of application we can assume there will be substantially more lookups on the ticket versus the ticket creations/updates.
<br>Additionally, analytics workloads and reporting tools will further drive up read pressure on the system.

<p align="left">
  <img src="docs/load-calculations.png" width="900" />
</p>

The ratio between reads and writes coupled with the fact that this threshold surpasses 500-1k req/sec is able to justify a CQRS architecture pattern.
![img_2.png](docs/l3-component-diagram.png)

The rest api call flow is simplified to only publish events to kafka. 
Seperate consumer services will process the events and persist the data to the database.

#### CQRS - Performance Metrics: Before vs After
The statistics below support the pattern selected for splitting the db entities between the Ticket Dispatcher and Query Service. (Source: Claude 4.0)
<br>**Single Database (Before):**
```
Total Capacity: 1000 IOPS
├── Writes: 200 IOPS (20%)
├── Reads: 800 IOPS (80%)
└── Contention: High (reads blocked by writes)

Query Performance:
├── Simple reads: 50ms (blocked by writes)
├── Complex reads: 500ms (competing for resources)
└── Peak hour degradation: 3x slower
```
**Separate Databases (After):**
```
Write Database: 1000 IOPS dedicated
├── Writes: 200 IOPS (20% utilized)
├── Spare capacity: 800 IOPS
└── Contention: None

Read Database: 2000 IOPS dedicated  
├── Reads: 800 IOPS (40% utilized)
├── Spare capacity: 1200 IOPS
└── Contention: None

Query Performance:
├── Simple reads: 5ms (no write interference)
├── Complex reads: 50ms (optimized indexes)
└── Peak hour performance: Consistent
```
-----

### 1.3 API and data model design
As derived by the requirements document. User should be able to:
- Create a ticket with minimal info
- Assign the ticket to a user
- Update ticket status
- Update ticket details (Out of scope, but crucial for MVP)

<br>The following are whiteboard API design sketches:
<p align="left">
  <img src="docs/api-design-1.png" width="900" />
</p>
<p align="left">
  <img src="docs/api-design-2.png" width="900" />
</p>
<p align="left">
  <img src="docs/api-design-3.png" width="900" />
</p>

The designs were fed into ChatGPT to generate the OpenAPI v2 schema file [TicketDispatcher-v1.0.yaml](docs/TicketDispatcher-v1.0.yaml). 
<br>The schemas generated had unresolved references and were fixed/enhanced appropriately.

<br>Below is the data model design:
<p align="left">
  <img src="docs/data-model-design.png" width="900" />
</p>

The designs were fed into ChatGPT to generate DB entities and repositories.

-----

### 1.4 Direct design impacts for the task.

#### 1.4.1 JDK choice
Due to the current stack in use, I will be developing this application using the latest jdk 8 stable version of spring boot : 2.7.18.
<br>Upgrading to JDK 21 LTS will give 2-3x better performance.
<br>Here are some considerations of why it's beneficial to upgrade to jdk 21 (Source: Claude 4.0):

1. Virtual Threads - JDK 21+ only
```java
// JDK 21 only
Executors.newVirtualThreadPerTaskExecutor()

// JDK 8 equivalent
ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
```

2. ZGC Garbage Collector- JDK 11+ only
```bash
# JDK 21
-XX:+UseZGC

# JDK 8 maximum
-XX:+UseG1GC
```

3. Modern JVM Flags - Many are JDK 11+ only

JDK 8 Performance Limitations:
```
- Throughput: ~10-15K req/sec (vs 25-30K on JDK 21)
- Memory: Need 8GB heap vs 4GB on JDK 21
- Threads: Limited to ~800-1000 platform threads
- GC Pauses: 10-50ms (vs <1ms on JDK 21)
```
#### 1.4.2 Idempotence 
Will be enforced through an Idempotency-Key requirement as a header request.
The key will be used and propagated to an eventId by the kafka publishers. 
For ticket creation flow, the kafka consumers will use this UUID as the ticketID value to persist to DB.
Using the eventId as the ticketId ensures end-to-end traceability across system boundaries. 
This enables consistent correlation between API requests, Kafka events, database records, and observability tools — greatly simplifying debugging, monitoring, and incident response.

#### 1.4.3 Tracing
- Client supplies an X-Correlation-ID header if it wants to track the full lifecycle.
- Server will generate a new one if missing.
- Server will always propagate it downstream (Kafka, logs, responses).

#### 1.4.4 Webflux (Reactive) vs traditional Async(thread-based concurrency)
(Source:Claude 4.0)
```REST API (WebFlux) → Kafka → Consumer Service (Traditional or Reactive)```

**Non-blocking I/O**:
- WebFlux uses event loops with far fewer threads (typically 2x CPU cores)
- Spring MVC blocks threads during Kafka publishing - you need thread pools sized for peak concurrent requests
- For 1000 concurrent requests: WebFlux uses ~8-16 threads, MVC needs ~1000 threads

**Memory Efficiency**:
- Each blocked thread in MVC consumes ~1MB stack space
- WebFlux shares threads across all requests
- 1000 concurrent: WebFlux ~16MB vs MVC ~1GB thread overhead

**Backpressure Handling**:
- WebFlux naturally handles slow Kafka brokers without blocking request threads
- MVC threads get stuck waiting, causing cascade failures under load

### 🆚 1.4.5 UUIDs vs. Sequential IDs as Primary Key
Source: [Baeldung](https://www.baeldung.com/uuid-vs-sequential-id-as-primary-key).

| Feature                   | `BIGINT` (e.g., `SERIAL`) | `UUIDv4`                 | `UUIDv7`                 |
| ------------------------- | ------------------------- | ------------------------ | ------------------------ |
| Size                      | 8 bytes                   | 16 bytes                 | 16 bytes                 |
| Index Fragmentation       | Low                       | High (random writes)     | Low (sequential writes)  |
| Join Performance          | Fast                      | Slow                     | Comparable to `BIGINT`   |
| Time-based Ordering       | ❌ No                      | ❌ No                     | ✅ Yes                    |
| Generation                | Centralized (DB)          | Distributed (clients ok) | Distributed (clients ok) |
| Predictability (security) | Easy to guess             | Hard to guess            | Hard to guess            |
| Readability               | Easy                      | Hard                     | Hard                     |

-----

## 2.2 OpenAPI (Swagger)—RESTful API specification
### Automated API changes - Swagger Codegen
All api changes and definitions are captured through [OpenAPI Specification (OAS) standard](https://swagger.io/specification/).
<br/>Transaction Server utilizes [swagger codegen library](https://github.com/swagger-api/swagger-codegen) to generate code based on this spec.
<br/>This generator can be used to create controller and model classes, which reduce the code changes required when changing a Rest Api spec.
<br/>The overall project structure shows which components will be auto-generated:
```
|-- src
|   `-- main
|       |-- java
|       |   `-- com.pleased.ticket.dispatcher.server
|       |               |-- controller (generated with custom swagger codegen - contains spring & springfox annotations)
|       |               |-- filter
|       |               |-- delegate
|       |               |-- service
|       |               |-- config
|       |               |-- exception
|       |               |-- model (generated with custom swagger codegen)
|       |               `-- util
|       `-- resources
```
***NOTE***
<br/> I have extensive experience using Swagger (now OpenAPI).
<br/> During my time at ScotiaBank, I served as the sole contributor to an open-source extension of Swagger Codegen.
<br/> My work involved creating custom mustache templates to support multiple use cases across North and South America, as well as enhancing internal libraries built on top of the Spring Framework.
<br/> For this project, I'm leveraging my own private, customized solution I developed based on Swagger Codegen to ensure robust integration and functionality.
<br/> This will ensure fast and easy API updates to the codebase straight from the spec changes.

### Swagger UI - Spring Fox
To view the swagger spec in the browser, go on localhost:8080/swagger-ui/.
<br> **Note**:Due to implementation of the reactive stack (spring webflux)- this needs configuration changes to activate.

## 2.3 API Updates
To perform api updates through the code base, please run maven build with codegen profile:
```bash
mvn clean package -P codegen
```

-----

## 2.4 Design Decisions and Best Practices
### Architecture Overview
We leverage proven design patterns to ensure a maintainable, extensible, and secure system:

* **Model‑View‑Controller**: Declarative URL mappings via annotations streamline request handling.
* **Dependency Injection**: Singleton beans and auto‑wiring decouple components and simplify testing.
* **Front Controller**: Centralized filters enforce logging, authentication, validation, and exception handling consistently.

### 1. Logging
**Objective**: Full visibility of HTTP exchanges—without sacrificing data privacy.

* A custom servlet filter captures every request and response.
* Global masking rules redact sensitive fields.
* Spring’s default web logging is disabled to prevent duplicates and standardize output format.
* Result: Uniform, high‑fidelity logs that balance auditability with GDPR‑style privacy controls.

### 2. Security Integration

**Objective**: Enterprise‑grade, performant API protection.

* Extend Spring Security’s filter chain to inject custom authentication and authorization logic.
* Leverage built‑in role‑based access control and security context propagation.
* Maintain lightweight filters to support high request throughput without latency penalties.

### 3. Validation

**Objective**: Shift validation to the edges for consistency and simplicity.

* Use Jackson annotations and Spring’s out‑of‑the‑box validator against the OpenAPI schema.
* Validation occurs during HTTP message conversion, before controller invocation.
* For complex business rules, implement `HandlerInterceptor`‑based validators to access handler metadata and Spring context post‑mapping.

### 4. Exception Handling

**Objective**: Centralize error management and minimize exposed details.

* Push error handling to the boundaries; avoid in‑line null checks and try/catch in business logic.
* Use `@ControllerAdvice` and extend `ResponseEntityExceptionHandler` to handle custom and framework exceptions uniformly.
* Central handler obfuscates sensitive information in responses, enhancing security.

### 5. Object Mapping

**Objective**: Efficient and clear DTO-to-domain transformations.

* **Fluid Java setters** for simple, IDE‑friendly model generation aligned with OpenAPI.
* **MapStruct** for complex mappings, reducing boilerplate and runtime overhead.
* Reserve Lombok for scenarios where annotation processing is fully supported and the team is aligned on its use.

### 6.Application properties
All sensitive properties like username passwords will be setup as kubernetes secrets in a cloud deployment environment.
<br/>Depending on the cloud provider, they would be linked with specific solutions
(HashiCorp Vault, AWS secret manager,Azure Key Vault,Google Cloud Secret Manager etc.)
<br/> Sensitive data can also be managed by jenkins pipeline for an on-prem solution,
and they can be injected during deployment using custom scripts.

For multi-tenant and multi-cluster complex deployments, I would highly recommend
applying Helm charts to manage the different configurations.

### 7.Third-party library usage.
This Section explains the reasoning behind selecting various libraries.

#### MapStruct vs other mapping libraries
(Source:Claude 3.5 Sonnet)

**Advantages**:
* Compile-time Validation
* Superior Performance
* Clear Error Messages
* Developer-Friendly

**Limitations**:
* Setup Requirements
* Learning Curve

**Performance Metrics**
```
MapStruct:    ~25,000,000 ops/sec
Manual:       ~24,000,000 ops/sec
ModelMapper:  ~500,000 ops/sec
Dozer:        ~100,000 ops/sec
```

#### SpringFox vs SpringDoc
SpringDoc is the new team, where old members from SpringFox moved to.
<br/>There are similar political reasons with what happened with Swagger and OpenAPI standard.
<br/>Due to the project requirement to use swagger and the adoption rate I selected the old version of swagger 2.0.

<p> Here's some more details on the Timeline & Evolution:
<br/> (Source:Claude 3.5 Sonnet)

* 2010-2011: Swagger Created
    - Originally developed by Wordnik
    - First major API documentation tool
    - Became very popular in API development

* 2015: SmartBear Acquisition
    - SmartBear acquired Swagger
    - Continued development under SmartBear

* 2016: OpenAPI Initiative (OAI)
    - Swagger Specification was donated to Linux Foundation
    - Renamed to OpenAPI Specification (OAS)
    - Became vendor-neutral standard

* Current State:
    - Swagger = SmartBear's toolset (SwaggerUI, Swagger Editor, etc.)
    - OpenAPI = The specification standard
    - OpenAPI 3.0+ = Current specification version

* Use SpringDoc for:
    - New projects
    - Spring Boot 2.6+
    - Need for OpenAPI 3.0
    - Active maintenance

* Use Springfox only if:
    - Legacy projects
    - Specific requirement for Swagger 2.0
    - Cannot upgrade existing codebase

#### Spring Boot DevTools Features
(Source:Claude 3.5 Sonnet)
<br/>Here's a concise breakdown of Spring Boot DevTools

**Benefits**:
* Restarts application when classpath changes (faster than manual restart)
* Browser auto-refresh when changes detected
* Development-friendly property defaults
* Disables template caching
* Enables debug logging
* H2 console enabled
* Remote debugging support
* Enhanced error pages
* Detailed error messages

**Limitations**:
* Not for production use
* Memory overhead
* Potential security risks
* Performance impact

## Contributing
This is a product of Gentian Licenji for a demo interview with Sporty Group.
<br/>Developers:
<br/>Gentian Licenji

## License
This project is provided without any licensing restrictions and is free to use, modify, and distribute.

## Project status
The project is being enhanced and updated with no fixed timeline.


## 📚 References & Links

### CQRS (Command Query Responsibility Segregation)
- https://www.researchgate.net/publication/383565977_CQRS_DESIGN_PATTERN_ENHANCING_MICROSERVICES_PERFORMANCE_AND_SCALABILITY
- https://ijrmeet.org/implementing-command-query-responsibility-segregation-cqrs-in-large-scale-systems/
- https://www.ijert.org/optimizing-performance-and-scalability-in-micro-services-with-cqrs-design
- https://ijmra.us/project%20doc/2024/IJME_SEPTEMBER2024/IJMIE10_Sep24_23042.pdf

### MapStruct vs. Reflection-Based Mappers
- https://www.baeldung.com/java-performance-mapping-frameworks
- https://stackoverflow.com/questions/60096412/is-it-good-to-upgrade-to-mapstruct-from-modelmapper
- https://dzone.com/articles/comparing-modelmapper-and-mapstruct-in-java-the-po
- https://www.javacodegeeks.com/2025/01/mapstruct-vs-modelmapper-a-comparative-analysis.html

### Java 21 Virtual Threads
- https://mariadb.com/resources/blog/benchmark-jdbc-connectors-and-java-21-virtual-threads/
- https://adi.earth/posts/database-virtual-threads-benchmark/
- https://stackoverflow.com/questions/78318131/do-java-21-virtual-threads-address-the-main-reason-to-switch-to-reactive-single
