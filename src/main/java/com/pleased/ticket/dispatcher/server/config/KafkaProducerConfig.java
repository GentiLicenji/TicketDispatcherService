//package com.pleased.ticket.dispatcher.server.config;
//
//
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.protocol.Message;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * High Throughput Kafka configuration
// */
//@Profile("!embedded-kafka") // active when NOT in test
//@Configuration
//@EnableKafka
//@Slf4j
//public class KafkaProducerConfig {
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;
//
//    @Value("${spring.kafka.schema-registry.url}")
//    private String schemaRegistryUrl;
//
//    @Bean
//    public ProducerFactory<String, Message> protobufProducerFactory() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
//        props.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
//        props.put(KafkaProtobufSerializerConfig.AUTO_REGISTER_SCHEMAS, true);
//
//        // Performance optimizations
//        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd");
//        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
//        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
//        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);
//        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
//        props.put(ProducerConfig.ACKS_CONFIG, "all");
//
//        // Custom partitioner for ticket events
//        props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, TicketPartitioner.class);
//
//        return new DefaultKafkaProducerFactory<>(props);
//    }
//
//    @Bean
//    public KafkaTemplate<String, Message> protobufKafkaTemplate() {
//        return new KafkaTemplate<>(protobufProducerFactory());
//    }
//}
//
