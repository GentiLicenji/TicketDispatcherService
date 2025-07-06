package com.pleased.ticket.dispatcher.server.config;


import com.pleased.ticket.dispatcher.server.util.mapper.UUIDConversion;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.specific.SpecificData;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.SenderOptions;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * High Throughput Kafka configuration
 */
@Profile("!embedded-kafka") // active when NOT in test
@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.schema-registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Bean
    public ReactiveKafkaProducerTemplate<ByteBuffer, Object> reactiveKafkaProducerTemplate() {
        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        // Schema Registry
        props.put("schema.registry.url", schemaRegistryUrl);

        // Transaction Configuration
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "reactive-producer-tx");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Required for transactions
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // Required for transactions

        // Performance tuning (adjusted for transactions)
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 131072);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.SEND_BUFFER_CONFIG, 131072);
        props.put(ProducerConfig.RECEIVE_BUFFER_CONFIG, 65536);

        // Timeout configurations
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        // Connection pooling
        props.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);

        SenderOptions<ByteBuffer, Object> senderOptions = SenderOptions.create(props);

        // Transactional reactive configurations
        senderOptions = senderOptions
                .maxInFlight(256) // Lower for transactional
                .stopOnError(true) // Stop on error for transactional integrity
                .scheduler(Schedulers.boundedElastic());

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    /**
     * Registering custom UUID conversion for avro.
     */
    @PostConstruct
    public void setupAvroConversions() {
        GenericData.get().addLogicalTypeConversion(new UUIDConversion());
        SpecificData.get().addLogicalTypeConversion(new UUIDConversion());
    }
}

