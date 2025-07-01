package com.pleased.ticket.dispatcher.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TICKET_CREATE_TOPIC = "ticket-create.v1";
    public static final String TICKET_ASSIGNMENTS_TOPIC = "ticket-assignments.v1";
    public static final String TICKET_UPDATES_TOPIC = "ticket-updates.v1";

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
}
