//package com.pleased.ticket.dispatcher.server.config;
//
//
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.context.annotation.Profile;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.ConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.ProducerFactory;
//import org.springframework.kafka.listener.ContainerProperties;
//import org.springframework.kafka.listener.DefaultErrorHandler;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//import org.springframework.kafka.transaction.KafkaTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//import org.springframework.util.backoff.FixedBackOff;
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//
//@Profile("!embedded-kafka") // active when NOT in test
//@Configuration
//@EnableKafka
//@Slf4j
//public class KafkaConsumerConfig {
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;
//
//    @Value("${spring.kafka.schema-registry.url}")
//    private String schemaRegistryUrl;
//
//    private Map<String, Object> getBaseConsumerProps() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
//        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
//        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000);
//        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
//        return props;
//    }
//
//    // TICKET CREATED CONSUMER - Lower batch size for complex processing
//    @Bean
//    @Primary
//    public ConsumerFactory<String, TicketEventsProto.TicketCreated> ticketCreatedConsumerFactory() {
//        Map<String, Object> props = getBaseConsumerProps();
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ticket-service-create-consumer-v2");
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // Lower for complex creates
//
//        return new DefaultKafkaConsumerFactory<>(
//                props,
//                new StringDeserializer(),
//                new ProtobufDeserializer<>(TicketEventsProto.TicketCreated.parser())
//        );
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, TicketEventsProto.TicketCreated>
//    ticketCreatedBatchListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, TicketEventsProto.TicketCreated> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(ticketCreatedConsumerFactory());
//        factory.setBatchListener(true);
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//        factory.setConcurrency(2); // Lower concurrency for complex processing
//
//        // Custom error handler for ticket creation
//        factory.setCommonErrorHandler(new DefaultErrorHandler(
//                new FixedBackOff(2000L, 3) // Longer backoff for creates
//        ));
//
//        return factory;
//    }
//
//    // TICKET ASSIGNED CONSUMER - Medium batch size
//    @Bean
//    public ConsumerFactory<String, TicketEventsProto.TicketAssigned> ticketAssignedConsumerFactory() {
//        Map<String, Object> props = getBaseConsumerProps();
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ticket-service-assignment-consumer-v2");
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 250);
//
//        return new DefaultKafkaConsumerFactory<>(
//                props,
//                new StringDeserializer(),
//                new ProtobufDeserializer<>(TicketEventsProto.TicketAssigned.parser())
//        );
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, TicketEventsProto.TicketAssigned>
//    ticketAssignedBatchListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, TicketEventsProto.TicketAssigned> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(ticketAssignedConsumerFactory());
//        factory.setBatchListener(true);
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//        factory.setConcurrency(3);
//
//        factory.setCommonErrorHandler(new DefaultErrorHandler(
//                new FixedBackOff(1000L, 2)
//        ));
//
//        return factory;
//    }
//
//    // TICKET STATUS UPDATED CONSUMER - Higher batch size for simple updates
//    @Bean
//    public ConsumerFactory<String, TicketEventsProto.TicketStatusUpdated> ticketStatusUpdatedConsumerFactory() {
//        Map<String, Object> props = getBaseConsumerProps();
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ticket-service-update-consumer-v2");
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Higher for simple updates
//
//        return new DefaultKafkaConsumerFactory<>(
//                props,
//                new StringDeserializer(),
//                new ProtobufDeserializer<>(TicketEventsProto.TicketStatusUpdated.parser())
//        );
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, TicketEventsProto.TicketStatusUpdated>
//    ticketStatusUpdatedBatchListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, TicketEventsProto.TicketStatusUpdated> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(ticketStatusUpdatedConsumerFactory());
//        factory.setBatchListener(true);
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//        factory.setConcurrency(4); // Higher concurrency for simple updates
//
//        factory.setCommonErrorHandler(new DefaultErrorHandler(
//                new FixedBackOff(500L, 2) // Faster retry for simple updates
//        ));
//
//        return factory;
//    }
//}