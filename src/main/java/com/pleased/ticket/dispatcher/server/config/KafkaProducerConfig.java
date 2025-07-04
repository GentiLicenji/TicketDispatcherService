package com.pleased.ticket.dispatcher.server.config;


import com.pleased.ticket.dispatcher.server.util.mapper.TicketEventSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;


import java.util.HashMap;
import java.util.Map;

/**
 * High Throughput Kafka configuration
 */
@Profile("!embedded-kafka") // active when NOT in test
@Configuration
//@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaProducerConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // High Throughput Optimizations
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 131072); // 128KB
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Wait 5m for faster response
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864); // 64MB buffer
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Fast compression
        props.put(ProducerConfig.SEND_BUFFER_CONFIG, 131072); // 128KB send buffer
        props.put(ProducerConfig.RECEIVE_BUFFER_CONFIG, 65536); // 64KB receive buffer

        // Performance tuning
        props.put(ProducerConfig.ACKS_CONFIG, "1"); // Only leader acknowledgment - Switch to ALL for production
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        // Connection pooling
        props.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

//        // Add type information to JSON
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // Don't use headers
        props.put(JsonSerializer.TYPE_MAPPINGS,
                "TicketCreated:com.pleased.ticket.dispatcher.server.model.events.TicketCreated," +
                        "TicketAssigned:com.pleased.ticket.dispatcher.server.model.events.TicketAssigned," +
                        "TicketStatusUpdated:com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated");

        // Idempotence for exactly-once semantics (optional, reduces throughput slightly)
//         props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

