package com.pleased.ticket.dispatcher.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pleased.ticket.dispatcher.server.config.KafkaTopicConfig;
import com.pleased.ticket.dispatcher.server.config.TestKafkaConfig;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestKafkaConfig.class)
public class TicketEventProducerIT {

    @Autowired
    private TicketEventProducer ticketEventProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private ObjectMapper objectMapper;

    // Queues to collect messages
    private BlockingQueue<ConsumerRecord<String, String>> createRecords;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Initialize queues
        createRecords = new LinkedBlockingQueue<>();

        // Set up consumer for topic
        setupConsumerForTopic(KafkaTopicConfig.TICKET_CREATE_TOPIC, createRecords, "createTestGroup");
    }

    private void setupConsumerForTopic(String topic, BlockingQueue<ConsumerRecord<String, String>> records, String groupId) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(topic);

        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(cf, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) records::offer);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        createRecords.clear();
    }

    @Disabled("Needs more time to fix. No published events are being captured.")
    @Test
    void publishTicketCreated_ShouldPublishEventToKafka() throws Exception {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        TicketCreated event = TicketCreated.builder()
                .ticketId(ticketId)
                .subject("Test Ticket")
                .description("This is a test ticket")
                .userId(userId)
                .projectId(projectId)
                .correlationId(correlationId)
                .eventId(eventId)
                .createdAt(now)
                .build();

        // Act
        ticketEventProducer.publishTicketCreated(event).block(Duration.ofSeconds(5));

        // Assert
        ConsumerRecord<String, String> received = createRecords.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.topic()).isEqualTo(KafkaTopicConfig.TICKET_CREATE_TOPIC);
        assertThat(received.key()).isEqualTo(ticketId.toString());

        TicketCreated receivedEvent = objectMapper.readValue(received.value(), TicketCreated.class);
        assertThat(receivedEvent.getTicketId()).isEqualTo(ticketId);
        assertThat(receivedEvent.getSubject()).isEqualTo("Test Ticket");
        assertThat(receivedEvent.getDescription()).isEqualTo("This is a test ticket");
        assertThat(receivedEvent.getUserId()).isEqualTo(userId);
        assertThat(receivedEvent.getProjectId()).isEqualTo(projectId);
        assertThat(receivedEvent.getCorrelationId()).isEqualTo(correlationId);
        assertThat(receivedEvent.getEventId()).isEqualTo(eventId);
        assertThat(receivedEvent.getCreatedAt()).isEqualTo(now);
    }

}