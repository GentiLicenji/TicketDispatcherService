package com.pleased.ticket.dispatcher.server.config;

import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import reactor.kafka.receiver.ReceiverOptions;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Profile("!embedded-kafka")
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.schema-registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    private static final int CONCURRENCY = 10;
    private static final long RETRY_ATTEMPTS = 3L;
    private static final long RETRY_INTERVAL = 1000L;

    /**
     * Base consumer properties shared across all consumers
     */
    private Map<String, Object> getBaseConsumerProperties(String groupId) {
        Map<String, Object> props = new HashMap<>();

        // Basic configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        // Consumer group and offset management
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reactive

        // Performance tuning
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576);

        return props;
    }

    /**
     * Create error handler with configurable retry settings
     */
    private DefaultErrorHandler createErrorHandler() {
        return new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    log.error("Error processing record: topic={}, partition={}, offset={}, error={}",
                            consumerRecord.topic(), consumerRecord.partition(),
                            consumerRecord.offset(), exception.getMessage(), exception);
                    // TODO: Send to DLQ or handle accordingly
                },
                new FixedBackOff(RETRY_INTERVAL, RETRY_ATTEMPTS)
        );
    }

    // ========================= CONTAINER FACTORIES =========================

    /**
     * Specific factory for TicketCreated events (if you need different group ID)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketCreated> ticketCreatedFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketCreated> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // Use specific group ID for ticket processing
        ConsumerFactory<String, TicketCreated> ticketConsumerFactory =
                new DefaultKafkaConsumerFactory<>(getBaseConsumerProperties("ticket-service-create-consumer"));

        factory.setConsumerFactory(ticketConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setCommonErrorHandler(createErrorHandler());
        factory.setMessageConverter(null);
        factory.setConcurrency(CONCURRENCY);

        return factory;
    }

    /**
     * Specific factory for TicketAssignment events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketAssigned> ticketAssignmentFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketAssigned> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // Use specific group ID for ticket assignment processing
        ConsumerFactory<String, TicketAssigned> assignmentConsumerFactory =
                new DefaultKafkaConsumerFactory<>(getBaseConsumerProperties("ticket-service-assignment-consumer"));

        factory.setConsumerFactory(assignmentConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setCommonErrorHandler(createErrorHandler());
        factory.setMessageConverter(null);
        factory.setConcurrency(CONCURRENCY);

        return factory;
    }

    /**
     * Specific factory for TicketUpdate events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketStatusUpdated> ticketUpdateFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketStatusUpdated> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // Use specific group ID for ticket update processing
        ConsumerFactory<String, TicketStatusUpdated> updateConsumerFactory =
                new DefaultKafkaConsumerFactory<>(getBaseConsumerProperties("ticket-service-update-consumer"));

        factory.setConsumerFactory(updateConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setCommonErrorHandler(createErrorHandler());
        factory.setMessageConverter(null);
        factory.setConcurrency(CONCURRENCY);

        return factory;
    }

    // ========================= REACTIVE KAFKA SUPPORT =========================

    /**
     * Single reactive Kafka consumer template for TicketCreated events
     * Control concurrency at the processing level, not consumer level
     */
    @Bean
    public ReactiveKafkaConsumerTemplate<ByteBuffer, TicketCreated> reactiveTicketCreatedConsumer() {
        Map<String, Object> props = getBaseConsumerProperties("ticket-service-create-consumer-reactive");

        ReceiverOptions<ByteBuffer, TicketCreated> receiverOptions = ReceiverOptions
                .<ByteBuffer, TicketCreated>create(props)
                .subscription(Collections.singleton(KafkaTopicConfig.TICKET_CREATE_TOPIC))
                .addAssignListener(partitions ->
                        log.info("Reactive ticket consumer assigned partitions: {}", partitions))
                .addRevokeListener(partitions ->
                        log.info("Reactive ticket consumer revoked partitions: {}", partitions))
                .commitInterval(Duration.ofSeconds(5))
                .commitBatchSize(100);

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }

    /**
     * Reactive Kafka consumer template for TicketAssignment events
     * Control concurrency at the processing level, not consumer level
     */
    @Bean
    public ReactiveKafkaConsumerTemplate<ByteBuffer, TicketAssigned> reactiveTicketAssignmentConsumer() {
        Map<String, Object> props = getBaseConsumerProperties("ticket-service-assignment-consumer-reactive");

        ReceiverOptions<ByteBuffer, TicketAssigned> receiverOptions = ReceiverOptions
                .<ByteBuffer, TicketAssigned>create(props)
                .subscription(Collections.singleton(KafkaTopicConfig.TICKET_ASSIGNMENTS_TOPIC))
                .addAssignListener(partitions ->
                        log.info("Reactive ticket assignment consumer assigned partitions: {}", partitions))
                .addRevokeListener(partitions ->
                        log.info("Reactive ticket assignment consumer revoked partitions: {}", partitions))
                .commitInterval(Duration.ofSeconds(5))
                .commitBatchSize(100);

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }

    /**
     * Reactive Kafka consumer template for TicketUpdate events
     * Control concurrency at the processing level, not consumer level
     */
    @Bean
    public ReactiveKafkaConsumerTemplate<ByteBuffer, TicketStatusUpdated> reactiveTicketUpdateConsumer() {
        Map<String, Object> props = getBaseConsumerProperties("ticket-service-update-consumer-reactive");

        ReceiverOptions<ByteBuffer, TicketStatusUpdated> receiverOptions = ReceiverOptions
                .<ByteBuffer, TicketStatusUpdated>create(props)
                .subscription(Collections.singleton(KafkaTopicConfig.TICKET_UPDATES_TOPIC))
                .addAssignListener(partitions ->
                        log.info("Reactive ticket update consumer assigned partitions: {}", partitions))
                .addRevokeListener(partitions ->
                        log.info("Reactive ticket update consumer revoked partitions: {}", partitions))
                .commitInterval(Duration.ofSeconds(5))
                .commitBatchSize(100);

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }
}