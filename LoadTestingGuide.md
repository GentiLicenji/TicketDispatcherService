# Kafka Local Dev Setup - Load Testing.

## ✅ My Setup Overview

| Component       | Location                             |
| --------------- | ------------------------------------ |
| Spring Boot App | Runs on Windows or WSL               |
| Kafka Cluster   | Runs in Docker (via Compose)         |
| Zookeeper       | Required for Kafka (via Docker)      |
| Goal            | Load test realistically, but locally |

---

## 🧱 Goal

1. Run a single high-performance Kafka broker in Docker (using `confluentinc/cp-kafka`)
2. **Expose Kafka brokers properly** so your Spring Boot app can connect
3. **Create topics with correct partitions & replication**
4. **Use your real payloads and Kafka transactional logic**
5. **Monitor performance (optional: Kafdrop)**

---

## ⚙️ Setup Plan

### 1. 🐳 Docker Compose – Single High-Throughput Broker

<details>
<summary><strong>📦 Click to view full <code>docker-compose.yml</code></strong></summary>

```yaml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      
      # Listen on two interfaces
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,PLAINTEXT_HOST://0.0.0.0:29092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT

      # THIS is key to allow internal broker-to-broker communication on correct interface
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_HOST

      # Single broker settings
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_DEFAULT_REPLICATION_FACTOR: 1
      KAFKA_MIN_INSYNC_REPLICAS: 1

      # High throughput optimizations
      KAFKA_NUM_NETWORK_THREADS: 8
      KAFKA_NUM_IO_THREADS: 16
      KAFKA_SOCKET_SEND_BUFFER_BYTES: 102400
      KAFKA_SOCKET_RECEIVE_BUFFER_BYTES: 102400
      KAFKA_SOCKET_REQUEST_MAX_BYTES: 104857600

      # Log settings for performance
      KAFKA_LOG_FLUSH_INTERVAL_MESSAGES: 10000
      KAFKA_LOG_FLUSH_INTERVAL_MS: 1000
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
      KAFKA_LOG_RETENTION_HOURS: 24

      # Auto topic creation
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_NUM_PARTITIONS: 12

      # JVM heap size for better performance
      KAFKA_HEAP_OPTS: "-Xmx2G -Xms2G"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - zookeeper

  kafdrop:
    image: obsidiandynamics/kafdrop
    ports:
      - "9000:9000"
    environment:
      KAFKA_BROKER_CONNECT: kafka:9092
    depends_on:
      - kafka
```
</details>

Make sure docker desktop is running on your system.
<br>To start container ,execute this on bash CLI:
```
docker-compose up -d
```

To check container runtime, execute this on bash CLI:
```
docker ps
```
---

### 2. ⚙️ Spring Boot Config
Follow producer configurations here: [KafkaProducerConfig](/src/main/java/com/pleased/ticket/dispatcher/server/config/KafkaProducerConfig.java)
<br>Follow producer configurations here: [KafkaConsumerConfig](/src/main/java/com/pleased/ticket/dispatcher/server/config/KafkaConsumerConfig.java)

---

### 3. 🧪 Load Testing Strategy

### Option 1: JMeter GUI

1. Open Apache JMeter
2. Load the test plan:

   ```
   /docs/pleased-ticketing-load-test.jmx
   ```
3. Configure number of threads, duration, etc.
4. Run the test and view results in the GUI

### Option 2: JMeter CLI

To run the test in headless mode:

```bash
jmeter -n -t docs/pleased-ticketing-load-test.jmx -l results.jtl
```

Results will be saved to `results.jtl`.

---

### 4. 🏗️ Auto-Create Topics Configuration
Follow [KafkaTopicConfig](/src/main/java/com/pleased/ticket/dispatcher/server/config/KafkaTopicConfig.java)

---

### 5. 🧩 Use Monitoring Tools (Optional)

* **Kafdrop**: [http://localhost:9000](http://localhost:9000) → View partitions, lag, topic data
* **Kafka CLI**: view offsets, consumer groups, etc.

---

## 📈 Expected Throughput
With this single-broker setup, you should achieve:

- Producer: 50K-100K+ messages/second
- Consumer: 30K-80K+ messages/second
- Latency: <10ms for end-to-end processing

Monitor via Kafdrop and adjust configurations based on your specific hardware capabilities.

## 🧠 Summary

| Task            | What to Use                                                 |
| --------------- | ----------------------------------------------------------- |
| Kafka Cluster   | Docker Compose (3 brokers with Confluent images)            |
| App Connection  | `localhost:9092,9093,9094`                                  |
| Load Generation | Spring Boot endpoints + JMeter/Postman, or CLI              |
| Topic Setup     | Pre-created with 12 partitions, RF=3                        |
| Monitoring      | Kafdrop, Kafka CLI                                          |
| Realistic Load  | 1000s of JSON messages, concurrent consumers, retries, etc. |
