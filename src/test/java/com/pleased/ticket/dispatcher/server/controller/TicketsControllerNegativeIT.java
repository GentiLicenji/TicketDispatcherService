package com.pleased.ticket.dispatcher.server.controller;

import com.pleased.ticket.dispatcher.server.config.DisableSecurityConfig;
import com.pleased.ticket.dispatcher.server.delegate.TicketsDelegate;
import com.pleased.ticket.dispatcher.server.exception.GlobalExceptionHandler;
import com.pleased.ticket.dispatcher.server.filter.CorrelationIdFilter;
import com.pleased.ticket.dispatcher.server.filter.IdempotencyFilter;
import com.pleased.ticket.dispatcher.server.filter.LoggingFilter;
import com.pleased.ticket.dispatcher.server.model.rest.TicketAssignmentRequest;
import com.pleased.ticket.dispatcher.server.model.rest.TicketCreateRequest;
import com.pleased.ticket.dispatcher.server.model.rest.TicketStatusRequest;
import com.pleased.ticket.dispatcher.server.service.TicketsApiService;
import com.pleased.ticket.dispatcher.server.util.mapper.TicketsMapperImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for the {@link TicketsController} focusing on negative scenarios and
 * request validation using Spring WebFlux.
 * <p>
 * This test class verifies that the ticket-related REST endpoints correctly handle
 * invalid input data, missing or malformed headers, and other client-side errors.
 * <p>
 * Authentication is explicitly disabled for these tests using {@link DisableSecurityConfig},
 * allowing direct testing of controller behavior without security filters.
 * <p>
 * Key test areas include:
 * <ul>
 *   <li>Validation of required fields in {@code TicketCreateRequest}, {@code TicketAssignmentRequest}, and {@code TicketStatusRequest}</li>
 *   <li>Header validation (e.g., malformed or missing UUIDs in {@code X-Correlation-ID}, {@code Idempotency-Key})</li>
 *   <li>UUID format enforcement on path and header parameters</li>
 *   <li>Edge cases like assigning a ticket twice or updating a closed ticket (disabled for decoupled flow)</li>
 * </ul>
 *
 * @see TicketsController
 * @see TicketsApiService
 * @see DisableSecurityConfig
 */
@ActiveProfiles("test")
@WebFluxTest
@ContextConfiguration(classes = {TicketsController.class,
        TicketsDelegate.class,
        CorrelationIdFilter.class,
        IdempotencyFilter.class,
        LoggingFilter.class,
        GlobalExceptionHandler.class,
        DisableSecurityConfig.class})
@AutoConfigureWebTestClient
@Import({TicketsMapperImpl.class}) // import the generated impl class
public class TicketsControllerNegativeIT {

    @MockBean
    TicketsApiService ticketsApiService;

    @Autowired
    private WebTestClient webTestClient;

    private static UUID ticketId;
    private static UUID projectId;
    private static UUID correlationId;
    private static UUID assigneeId;
    private static UUID idempotencyKey;

    @BeforeAll
    static void setUp() {
        ticketId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        correlationId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
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
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithMissingSubject_ShouldReturnBadRequest() {
        // Arrange

        TicketCreateRequest request = new TicketCreateRequest();
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());
        // Missing subject

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithMissingDescription_ShouldReturnBadRequest() {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setProjectId(projectId.toString());

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithInvalidProjectIdFormat_ShouldReturnBadRequest() {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId("not-a-valid-uuid");

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithNullProjectId_ShouldReturnBadRequest() {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(null);

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void assignTicket_WithInvalidTicketIdFormat_ShouldReturnBadRequest() {
        // Arrange
        String invalidTicketId = "not-a-valid-uuid";

        TicketAssignmentRequest assignRequest = new TicketAssignmentRequest();
        assignRequest.setAssigneeId(assigneeId.toString());

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets/" + invalidTicketId + "/assign")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(assignRequest))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void assignTicket_WithInvalidAssigneeIdFormat_ShouldReturnBadRequest() {

        UUID fictionalTicketId = UUID.randomUUID();

        // Arrange invalid assignment
        String invalidAssigneeId = "not-a-valid-uuid";
        UUID assignmentCorrelationId = UUID.randomUUID();
        UUID assignmentIdempotencyKey = UUID.randomUUID();

        TicketAssignmentRequest assignRequest = new TicketAssignmentRequest();
        assignRequest.setAssigneeId(invalidAssigneeId);

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets/" + fictionalTicketId + "/assign")
                .header("Authorization", "dummy auth")
                .header("X-Correlation-ID", assignmentCorrelationId.toString())
                .header("Idempotency-Key", assignmentIdempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(assignRequest))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void assignTicket_WithMissingAssigneeId_ShouldReturnBadRequest() {

        TicketAssignmentRequest assignRequest = new TicketAssignmentRequest();
        // Missing assigneeId

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets/" + ticketId + "/assign")
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(assignRequest))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    /**
     * Interesting scenario in rest validation.
     * <p>
     * Jackson fails to map the invalid enum value → sets status = null
     * <p>
     * Now @NotNull kicks in and throws a validation error as if it was null value.
     */
    @Test
    void updateTicketStatus_WithInvalidStatus_ShouldReturnBadRequest() {

        // Arrange: manually craft invalid payload
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("status", "INVALID_STATUS");

        // Act & Assert
        webTestClient.patch()
                .uri("/api/v1/tickets/" + ticketId + "/status")
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(invalidPayload))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void updateTicketStatus_WithMissingStatus_ShouldReturnBadRequest() {

        TicketStatusRequest statusRequest = new TicketStatusRequest();
        // Missing status

        // Act & Assert
        webTestClient.patch()
                .uri("/api/v1/tickets/" + ticketId + "/status")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(statusRequest))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithMissingIdempotencyKey_ShouldReturnBadRequest() {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act & Assert - Missing Idempotency-Key header
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Missing request header 'Idempotency-Key'"));
    }

    /**
     * Interesting finding. The two exceptions below are identical. They fail in converting invalid UUIDs.
     * <p>
     * However, they trigger different binding errors:
     * <p>
     * Optional headers with invalid formats → WebExchangeBindException (binding error)
     * <p>
     * Required headers with invalid formats → ServerWebInputException (input processing error)
     */
    @Test
    void createTicket_WithInvalidCorrelationIdFormat_ShouldReturnBadRequest() {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act & Assert - Invalid X-Correlation-ID format
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("X-Correlation-ID", "not-a-valid-uuid")
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithInvalidIdempotencyKeyFormat_ShouldReturnBadRequest() {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act & Assert - Invalid Idempotency-Key format
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", "not-a-valid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Type mismatch."));
    }

    @Test
    void createTicket_WithEmptyRequestBody_ShouldReturnBadRequest() {

        // Act & Assert - Empty request body
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("{}"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithExcessivelyLongSubject_ShouldReturnBadRequest() {

        char[] longSubject = new char[251];
        Arrays.fill(longSubject, 'A');

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject(new String(longSubject)); // Assuming max length is 255
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    @Test
    void createTicket_WithExcessivelyLongDescription_ShouldReturnBadRequest() {

        char[] longDescription = new char[5001];
        Arrays.fill(longDescription, 'A');

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription(new String(longDescription)); // Assuming max length is 5000
        request.setProjectId(projectId.toString());

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "dummy auth")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(containsString("Validation failed. Check logs for more details."));
    }

    /**
     * Test Kafka failure scenarios - These tests would require mocking Kafka
     * or using a test container with controlled failure injection
     */

    // Note: The following tests would require additional setup to simulate Kafka failures
    // They are included as examples of what should be tested in a complete test suite

    /*
    @Test
    void createTicket_WithKafkaUnavailable_ShouldReturnServiceUnavailable() {
        // This test would require mocking Kafka to simulate unavailability
        // Implementation would depend on your error handling strategy
        // - Retry mechanism
        // - Circuit breaker pattern
        // - Graceful degradation
    }

    @Test
    void createTicket_WithKafkaPublishFailure_ShouldHandleGracefully() {
        // This test would verify behavior when Kafka publish fails
        // - Should the ticket creation be rolled back?
        // - Should it be stored for later retry?
        // - Should it return an error to the client?
    }
    */

    /**
     * Additional edge case tests
     */
    @Disabled("Not valid test case. This can't happen because a different decoupled service will attempt the assignment.")
    @Test
    void assignTicket_ToAlreadyAssignedTicket_ShouldReturnConflict() {

        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setSubject("Test Ticket for Double Assignment");
        createRequest.setDescription("This is a test ticket for double assignment");
        createRequest.setProjectId(projectId.toString());

        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createRequest))
                .exchange()
                .expectStatus().isOk();

        // First assignment
        UUID firstAssigneeId = UUID.randomUUID();
        UUID firstAssignmentCorrelationId = UUID.randomUUID();
        UUID firstAssignmentIdempotencyKey = UUID.randomUUID();

        TicketAssignmentRequest firstAssignRequest = new TicketAssignmentRequest();
        firstAssignRequest.setAssigneeId(firstAssigneeId.toString());

        webTestClient.post()
                .uri("/api/v1/tickets/" + ticketId + "/assign")
                .header("X-Correlation-ID", firstAssignmentCorrelationId.toString())
                .header("Idempotency-Key", firstAssignmentIdempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(firstAssignRequest))
                .exchange()
                .expectStatus().isOk();

        // Second assignment (should conflict or reassign based on business rules)
        UUID secondAssigneeId = UUID.randomUUID();
        UUID secondAssignmentCorrelationId = UUID.randomUUID();
        UUID secondAssignmentIdempotencyKey = UUID.randomUUID();

        TicketAssignmentRequest secondAssignRequest = new TicketAssignmentRequest();
        secondAssignRequest.setAssigneeId(secondAssigneeId.toString());

        // Act & Assert - This behavior depends on your business rules
        // Option 1: Allow reassignment
        // Option 2: Return conflict
        webTestClient.post()
                .uri("/api/v1/tickets/" + ticketId + "/assign")
                .header("X-Correlation-ID", secondAssignmentCorrelationId.toString())
                .header("Idempotency-Key", secondAssignmentIdempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(secondAssignRequest))
                .exchange()
                .expectStatus().isEqualTo(409) // Assuming conflict is returned
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.error").value(containsString("Ticket is already assigned"));
    }

    @Disabled("Not valid test case. This can't happen because a different decoupled service will attempt the update.")
    @Test
    void updateTicketStatus_OnClosedTicket_ShouldReturnConflict() {

        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setSubject("Test Ticket for Closed Status");
        createRequest.setDescription("This is a test ticket for closed status");
        createRequest.setProjectId(projectId.toString());

        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createRequest))
                .exchange()
                .expectStatus().isOk();

        // Close the ticket
        UUID closeCorrelationId = UUID.randomUUID();
        UUID closeIdempotencyKey = UUID.randomUUID();

        TicketStatusRequest closeRequest = new TicketStatusRequest();
        closeRequest.setStatus(TicketStatusRequest.StatusEnum.CLOSED);

        webTestClient.patch()
                .uri("/api/v1/tickets/" + ticketId + "/status")
                .header("X-Correlation-ID", closeCorrelationId.toString())
                .header("Idempotency-Key", closeIdempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(closeRequest))
                .exchange()
                .expectStatus().isOk();

        // Try to update status on closed ticket
        UUID updateCorrelationId = UUID.randomUUID();
        UUID updateIdempotencyKey = UUID.randomUUID();

        TicketStatusRequest updateRequest = new TicketStatusRequest();
        updateRequest.setStatus(TicketStatusRequest.StatusEnum.IN_PROGRESS);

        // Act & Assert - Assuming closed tickets cannot be reopened
        webTestClient.patch()
                .uri("/api/v1/tickets/" + ticketId + "/status")
                .header("X-Correlation-ID", updateCorrelationId.toString())
                .header("Idempotency-Key", updateIdempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updateRequest))
                .exchange()
                .expectStatus().isEqualTo(409) // Assuming conflict is returned
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.error").value(containsString("Cannot update status of closed ticket"));
    }

    @Disabled("Not valid test case. This can't happen because a different decoupled service will attempt the update.")
    @Test
    void updateTicketStatus_WithNonExistentTicketId_ShouldReturnNotFound() {
        // Arrange
        UUID nonExistentTicketId = UUID.randomUUID();

        TicketStatusRequest statusRequest = new TicketStatusRequest();
        statusRequest.setStatus(TicketStatusRequest.StatusEnum.CLOSED);

        // Act & Assert
        webTestClient.patch()
                .uri("/api/v1/tickets/" + nonExistentTicketId + "/status")
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(statusRequest))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").exists();
    }
}