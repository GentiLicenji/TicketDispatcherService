package com.pleased.ticket.dispatcher.server;

import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.rest.TicketCreateRequest;
import com.pleased.ticket.dispatcher.server.model.rest.TicketResponse;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TicketsAPIE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    private Consumer<String, Object> kafkaConsumer;
    private static final String TICKET_CREATE_TOPIC = "ticket-create.v1";
    private static final String TICKET_ASSIGNMENTS_TOPIC = "ticket-assignments.v1";
    private static final String TICKET_UPDATES_TOPIC = "ticket-updates.v1";
    private static final String TEST_GROUP_ID = "test-consumer-group";

    @BeforeEach
    void setUp() {
        // Configure Kafka consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pleased.ticket.dispatcher.server.model.events");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.pleased.ticket.dispatcher.server.model.events.TicketEvent");

        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Arrays.asList(TICKET_CREATE_TOPIC, TICKET_ASSIGNMENTS_TOPIC, TICKET_UPDATES_TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    @Test
    void createTicket_ShouldReturnOkAndPublishEvent() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act - No Authorization header needed for local dev
        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                // Assert
                .expectStatus().isOk()
                .expectBody(TicketResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getTicketId());
                    assertEquals(request.getSubject(), response.getSubject());
                    assertEquals(request.getDescription(), response.getDescription());
                    assertEquals(projectId.toString(), response.getProjectId());
                    assertEquals("OPEN", response.getStatus().toString());
                });

        // Verify Kafka event
        ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofSeconds(5));
        boolean eventFound = false;
        Iterator<ConsumerRecord<String, Object>> iterator = records.iterator();
        while (iterator.hasNext()) {
            ConsumerRecord<String, Object> record = iterator.next();
            if (TICKET_CREATE_TOPIC.equals(record.topic()) && record.value() instanceof TicketCreated) {
                TicketCreated event = (TicketCreated) record.value();
                assertEquals(ticketId, event.getTicketId());
                assertEquals(request.getSubject(), event.getSubject());
                assertEquals(request.getDescription(), event.getDescription());
                assertEquals(projectId, event.getProjectId());
                assertEquals(correlationId, event.getCorrelationId());
                eventFound = true;
                break;
            }
        }
        assertTrue(eventFound, "TicketCreated event not found in Kafka");
    }

}
