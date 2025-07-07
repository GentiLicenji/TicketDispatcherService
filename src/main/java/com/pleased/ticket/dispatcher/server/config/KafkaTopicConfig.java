package com.pleased.ticket.dispatcher.server.config;

import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.Schema;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Profile("!embedded-kafka") // active when NOT in test
@Configuration
public class KafkaTopicConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicConfig.class);

    public static final String TICKET_CREATE_TOPIC = "ticket-create.v1";
    public static final String TICKET_ASSIGNMENTS_TOPIC = "ticket-assignments.v1";
    public static final String TICKET_UPDATES_TOPIC = "ticket-updates.v1";

    public static final Map<String, String> EVENT_TYPE_MAP;
    static {
        Map<String, String> temp = new HashMap<>();
        temp.put(TICKET_CREATE_TOPIC, "TicketCreated");
        temp.put(TICKET_ASSIGNMENTS_TOPIC, "TicketAssigned");
        temp.put(TICKET_UPDATES_TOPIC, "TicketStatusUpdated");
        EVENT_TYPE_MAP = Collections.unmodifiableMap(temp);
    }

    @Bean
    public NewTopic ticketCreateTopic() {
        return TopicBuilder.name(TICKET_CREATE_TOPIC)
                .partitions(12)  // High partition count for throughput
                .replicas(1)     // Single replica for single broker
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4")
                .config(TopicConfig.SEGMENT_MS_CONFIG, "600000") // 10 minutes
                .build();
    }

    @Bean
    public NewTopic ticketAssignmentsTopic() {
        return TopicBuilder.name(TICKET_ASSIGNMENTS_TOPIC)
                .partitions(12)
                .replicas(1)
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4")
                .config(TopicConfig.SEGMENT_MS_CONFIG, "600000")
                .build();
    }

    @Bean
    public NewTopic ticketUpdatesTopic() {
        return TopicBuilder.name(TICKET_UPDATES_TOPIC)
                .partitions(12)
                .replicas(1)
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4")
                .config(TopicConfig.SEGMENT_MS_CONFIG, "600000")
                .build();
    }

    /**
     * Avro Schema registration
     */
    private final SchemaRegistryClient client = new CachedSchemaRegistryClient("http://localhost:8081", 100);

    @PostConstruct
    public void registerSchemas() throws IOException, RestClientException {
        register(TICKET_CREATE_TOPIC + "-value", TicketCreated.SCHEMA$);
        register(TICKET_ASSIGNMENTS_TOPIC + "-value", TicketAssigned.SCHEMA$);
        register(TICKET_UPDATES_TOPIC + "-value", TicketStatusUpdated.SCHEMA$);
    }

    private void register(String subject, Schema schema) throws IOException, RestClientException {
        int id = client.register(subject, schema);
        logger.info("Registered schema under subject={} with id={}", subject, id);
    }
}
