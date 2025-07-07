package com.pleased.ticket.dispatcher.server.config;

import com.pleased.ticket.dispatcher.server.util.mapper.UUIDConversion;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificData;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Profile("embedded-kafka") //Active when testing
@TestConfiguration
public class TestKafkaConfig {

    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://test-registry";
    private static final String TEST_CONSUMER_GROUP = "test-reactive-consumer-group";

    // Embedded Kafka Broker with topic
    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaBroker(1, true,
                KafkaTopicConfig.TICKET_CREATE_TOPIC);
    }

    // Kafka Admin to create topics on embedded broker
    @Bean
    public KafkaAdmin kafkaAdmin(EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        return new KafkaAdmin(configs);
    }

    // Reactive Kafka Producer Template (matching main config structure)
    @Bean
    public ReactiveKafkaProducerTemplate<ByteBuffer, Object> reactiveKafkaProducerTemplate(
            EmbeddedKafkaBroker embeddedKafkaBroker) {

        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        // Mock Schema Registry for testing
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        // Transaction Configuration (simplified for testing)
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "test-reactive-producer-tx");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        // Performance tuning (reduced for testing)
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB for testing
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1); // Reduced for faster tests
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.SEND_BUFFER_CONFIG, 65536); // 64KB send buffer
        props.put(ProducerConfig.RECEIVE_BUFFER_CONFIG, 32768); // 32KB receive buffer

        // Timeout configurations (reduced for testing)
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000); // 10 seconds
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000); // 30 seconds

        // Connection pooling
        props.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 60000); // 1 minute

        SenderOptions<ByteBuffer, Object> senderOptions = SenderOptions.create(props);

        // Reactive configurations (adjusted for testing)
        senderOptions = senderOptions
                .maxInFlight(64) // Reduced for testing
                .stopOnError(true)
                .scheduler(Schedulers.boundedElastic());

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    // Reactive Kafka Consumer (ReceiverOptions)
    @Bean
    public ReceiverOptions<ByteBuffer, GenericRecord> reactiveKafkaReceiverOptions(
            EmbeddedKafkaBroker embeddedKafkaBroker) {

        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_CONSUMER_GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

        // Mock Schema Registry for testing
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        // Consumer specific configurations
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reactive
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        ReceiverOptions<ByteBuffer, GenericRecord> receiverOptions = ReceiverOptions.create(props);

        return receiverOptions
                .subscription(Collections.singleton(KafkaTopicConfig.TICKET_CREATE_TOPIC))
                .commitInterval(Duration.ofSeconds(1))
                .commitBatchSize(10)
                .maxCommitAttempts(3)
                .schedulerSupplier(() -> Schedulers.boundedElastic());
    }

    // Reactive Kafka Receiver Bean
    @Bean
    public KafkaReceiver<ByteBuffer, GenericRecord> reactiveKafkaReceiver(
            ReceiverOptions<ByteBuffer, GenericRecord> receiverOptions) {
        return KafkaReceiver.create(receiverOptions);
    }

    /**
     * Alternative simple receiver options with String key for easier testing
     */
    @Bean
    public ReceiverOptions<String, GenericRecord> simpleReactiveKafkaReceiverOptions(
            EmbeddedKafkaBroker embeddedKafkaBroker) {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-simple-consumer-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        ReceiverOptions<String, GenericRecord> receiverOptions = ReceiverOptions.create(props);

        return receiverOptions
                .subscription(Collections.singleton(KafkaTopicConfig.TICKET_CREATE_TOPIC))
                .commitInterval(Duration.ofSeconds(1))
                .commitBatchSize(10)
                .schedulerSupplier(() -> Schedulers.boundedElastic());
    }

    @Bean
    public KafkaReceiver<String, GenericRecord> simpleReactiveKafkaReceiver(
            ReceiverOptions<String, GenericRecord> simpleReceiverOptions) {
        return KafkaReceiver.create(simpleReceiverOptions);
    }

    /**
     * Initialize Mock Schema Registry for testing
     */
    @PostConstruct
    public void setupMockSchemaRegistry() {
        // Initialize mock schema registry
        MockSchemaRegistry.dropScope(MOCK_SCHEMA_REGISTRY_URL);

        // Setup Avro conversions
        GenericData.get().addLogicalTypeConversion(new UUIDConversion());
        SpecificData.get().addLogicalTypeConversion(new UUIDConversion());
    }
}