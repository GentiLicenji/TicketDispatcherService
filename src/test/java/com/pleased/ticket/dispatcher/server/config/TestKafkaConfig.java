package com.pleased.ticket.dispatcher.server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

@TestConfiguration
public class TestKafkaConfig {
    @Bean
    @Primary
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaBroker(1, true, "ticket-create.v1", "ticket-assignments.v1", "ticket-updates.v1");
    }
}
