package com.pleased.ticket.dispatcher.server.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class TestKafkaConfig {
    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaBroker(2, true, KafkaTopicConfig.TICKET_CREATE_TOPIC)
                .brokerProperty("log.dir", "target/embedded-kafka");
    }

    @Bean
    public KafkaAdmin kafkaAdmin(EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                embeddedKafkaBroker.getBrokersAsString());
        return new KafkaAdmin(configs);
    }
}
