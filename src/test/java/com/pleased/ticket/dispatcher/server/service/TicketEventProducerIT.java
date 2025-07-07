package com.pleased.ticket.dispatcher.server.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pleased.ticket.dispatcher.server.config.KafkaTopicConfig;
import com.pleased.ticket.dispatcher.server.config.TestKafkaConfig;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.util.mapper.UUIDConverter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

//TODO: needs fixing and re-testing.
@ActiveProfiles("embedded-kafka")
@SpringBootTest
@Import(TestKafkaConfig.class)
@Disabled("Needs fixing after reactive changes with Kafka test config.")
public class TicketEventProducerIT {

    @Autowired
    private TicketEventProducer ticketEventProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    private ObjectMapper objectMapper;

    // Queues to collect messages
    private BlockingQueue<ConsumerRecord<String, String>> createRecords;

    // Container as instance variable so we can stop and clear it
    private KafkaMessageListenerContainer<String, String> container;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Initialize queues
        createRecords = new LinkedBlockingQueue<>();

        // Set up consumer for topic
        setupConsumerForTopic(createRecords);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        createRecords.clear();
    }

    private void setupConsumerForTopic(BlockingQueue<ConsumerRecord<String, String>> records) {
        ContainerProperties containerProperties = new ContainerProperties(KafkaTopicConfig.TICKET_CREATE_TOPIC);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) records::offer);
        container.start();

        // Wait for assignment with timeout
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // Add a small delay to ensure consumer is ready
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void publishTicketCreated_ShouldPublishEventToKafka() throws Exception {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        TicketCreated event = TicketCreated.newBuilder()
                .setTicketId(UUIDConverter.uuidToBytes(ticketId))
                .setSubject("Test Ticket")
                .setDescription("This is a test ticket")
                .setUserId(UUIDConverter.uuidToBytes(userId))
                .setProjectId(UUIDConverter.uuidToBytes(projectId))
//                .setCorrelationId(correlationId)
                .setEventId(UUIDConverter.uuidToBytes(eventId))
                .setCreatedAt(now.toInstant())
                .build();

        // Act
        ticketEventProducer.publishTicketCreated(event,correlationId).block(Duration.ofSeconds(5));

        // Assert
        ConsumerRecord<String, String> received = createRecords.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.topic()).isEqualTo(KafkaTopicConfig.TICKET_CREATE_TOPIC);
        assertThat(received.key()).isEqualTo(ticketId.toString());

        TicketCreated receivedEvent = objectMapper.convertValue(received.value(), TicketCreated.class);
        assertThat(receivedEvent.getTicketId()).isEqualTo(ticketId);
        assertThat(receivedEvent.getSubject()).isEqualTo("Test Ticket");
        assertThat(receivedEvent.getDescription()).isEqualTo("This is a test ticket");
        assertThat(receivedEvent.getUserId()).isEqualTo(userId);
        assertThat(receivedEvent.getProjectId()).isEqualTo(projectId);
//        assertThat(receivedEvent.getCorrelationId()).isEqualTo(correlationId);
        assertThat(receivedEvent.getEventId()).isEqualTo(eventId);
        assertThat(receivedEvent.getCreatedAt()).isEqualTo(now);
    }
}