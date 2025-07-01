package com.pleased.ticket.dispatcher.server.controller;

import com.pleased.ticket.dispatcher.server.config.DisableSecurityConfig;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.model.rest.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test Suite for Tickets API Controller Endpoints
 *
 * <p>This test suite performs comprehensive integration testing of the ticket processing
 * endpoints, including validation of business rules, error handling, and successful flows.
 * Tests are executed with an H2 in-memory database.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>Uses H2 database configuration from {@link TestConfig}</li>TODO: update docs
 *   <li>Runs with 'test' profile loading application-test.properties</li>
 *   <li>Disabled security filters for focused controller testing</li>
 * </ul>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Positive Scenarios:
 *     <ul>
 *       <li>Successful transaction creation (deposit/withdrawal)</li>
 *       <li>Balance management within limits</li>
 *       <li>Transaction status verification</li>
 *     </ul>
 *   </li>
 *   <li>Negative Scenarios:
 *     <ul>
 *       <li>Invalid transaction amounts</li>
 *       <li>Non-existent accounts</li>
 *       <li>Rate limiting violations</li>
 *       <li>Insufficient balance handling</li>
 *       <li>Minimum balance violations</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Profile("test")
@Import(DisableSecurityConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TicketsControllerIT {

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

    /**
     * ******************
     * Positive Scenarios
     * ******************
     */
    @Test
    void createTicket_WithValidPayload_ShouldReturnOkAndPublishEvent() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act
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

    @Test
    void assignTicket_WithValidAuth_ShouldReturnOkAndPublishEvent() {
        // First create a ticket
        UUID ticketId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setSubject("Test Ticket for Assignment");
        createRequest.setDescription("This is a test ticket for assignment");
        createRequest.setProjectId(projectId.toString());

        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createRequest))
                .exchange()
                .expectStatus().isOk();

        // Now assign the ticket
        UUID assigneeId = UUID.randomUUID();
        UUID assignmentCorrelationId = UUID.randomUUID();
        UUID assignmentIdempotencyKey = UUID.randomUUID();

        TicketAssignmentRequest assignRequest = new TicketAssignmentRequest();
        assignRequest.setAssigneeId(assigneeId.toString());

        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets/" + ticketId + "/assign")
                .header("X-Correlation-ID", assignmentCorrelationId.toString())
                .header("Idempotency-Key", assignmentIdempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(assignRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketAssignmentResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(ticketId.toString(), response.getTicketId());
                    assertNotNull(response.getAssignee());
                    assertEquals(assigneeId.toString(), response.getAssignee().getUserId().toString());
                });

        // Verify Kafka event
        ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofSeconds(5));
        boolean eventFound = false;
        Iterator<ConsumerRecord<String, Object>> iterator = records.iterator();
        while (iterator.hasNext()) {
            ConsumerRecord<String, Object> record = iterator.next();
            if (TICKET_ASSIGNMENTS_TOPIC.equals(record.topic()) && record.value() instanceof TicketAssigned) {
                TicketAssigned event = (TicketAssigned) record.value();
                assertEquals(ticketId, event.getTicketId());
                assertEquals(assigneeId, event.getAssigneeId());
                assertEquals(assignmentCorrelationId, event.getCorrelationId());
                eventFound = true;
                break;
            }
        }
        assertTrue(eventFound, "TicketAssigned event not found in Kafka");
    }

    @Test
    void updateTicketStatus_WithValidAuth_ShouldReturnOkAndPublishEvent() {
        // First create a ticket
        UUID ticketId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setSubject("Test Ticket for Status Update");
        createRequest.setDescription("This is a test ticket for status update");
        createRequest.setProjectId(projectId.toString());

        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createRequest))
                .exchange()
                .expectStatus().isOk();

        // Now update the ticket status
        UUID statusUpdateCorrelationId = UUID.randomUUID();
        UUID statusUpdateIdempotencyKey = UUID.randomUUID();
        String newStatus = "CLOSED";

        TicketStatusRequest statusRequest = new TicketStatusRequest();
        statusRequest.setStatus(newStatus);

        webTestClient.patch()
                .uri("http://localhost:" + port + "/api/v1/tickets/" + ticketId + "/status")
                .header("X-Correlation-ID", statusUpdateCorrelationId.toString())
                .header("Idempotency-Key", statusUpdateIdempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(statusRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketStatusResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(ticketId.toString(), response.getTicketId());
                    assertEquals(newStatus, response.getStatus());
                });

        // Verify Kafka event
        ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofSeconds(5));
        boolean eventFound = false;
        Iterator<ConsumerRecord<String, Object>> iterator = records.iterator();
        while (iterator.hasNext()) {
            ConsumerRecord<String, Object> record = iterator.next();
            if (TICKET_UPDATES_TOPIC.equals(record.topic()) && record.value() instanceof TicketStatusUpdated) {
                TicketStatusUpdated event = (TicketStatusUpdated) record.value();
                assertEquals(ticketId, event.getTicketId());
                assertEquals(newStatus, event.getStatus());
                assertEquals(statusUpdateCorrelationId, event.getCorrelationId());
                eventFound = true;
                break;
            }
        }
        assertTrue(eventFound, "TicketStatusUpdated event not found in Kafka");
    }

    /**
     * ******************
     * Negative Scenarios
     * ******************
     */
    @Test
    void createTicket_WithMissingFields_ShouldReturnBadRequest() {
        // Arrange
        TicketCreateRequest request = new TicketCreateRequest();
        // Missing required fields

        // Act & Assert
        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest();
    }
//    TODO: Add negative scenarios
//    Test negative case for deduplication logic on REST.(two identical ticketCreate should result in a single service call!)
//    Test validation errors by giving wrong project-id  as non UUID type.
//    Test validation errors by missing required fields.
//    Test kafka failures with expected error.
}