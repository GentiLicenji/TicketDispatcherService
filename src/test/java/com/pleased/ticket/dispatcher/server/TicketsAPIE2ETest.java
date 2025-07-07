package com.pleased.ticket.dispatcher.server;

import com.nimbusds.jose.JOSEException;
import com.pleased.ticket.dispatcher.server.model.dto.ProjectEntity;
import com.pleased.ticket.dispatcher.server.model.dto.UserEntity;
import com.pleased.ticket.dispatcher.server.model.rest.*;
import com.pleased.ticket.dispatcher.server.repository.ProjectRepository;
import com.pleased.ticket.dispatcher.server.repository.TicketRepository;
import com.pleased.ticket.dispatcher.server.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("To be triggered manually.Integration tests depend on a live Kafka instance (broker) to validate producer/consumer behavior.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//Enforces JUnit 5 to reuse a single instance of the test class for all test methods
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Enforces Junit 5 to preserve execution order
public class TicketsAPIE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebTestClient webTestClient;

    private static UUID ticketId;
    private static UUID userId;
    private static UUID projectId;
    private static UUID assigneeId;

    /**
     * Create the required user,assignee and project for all tests.
     * <p>
     * Note: even though H2 is an in memory db, and it shouldn't persist data.
     * Spring test stores the same context between all test calls.
     */
    @BeforeAll
    void setUp() {
        //Cleanup from previous runs
        ticketRepository.deleteAll().block();
        userRepository.deleteAll().block();
        projectRepository.deleteAll().block();

        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        //Creating db entries for test setup
        UserEntity pmUser = new UserEntity();
        pmUser.setUserId(userId);
        pmUser.setEmail("test@example.com");
        pmUser.setName("Test PM");

        UserEntity devUser = new UserEntity();
        devUser.setUserId(assigneeId);
        devUser.setEmail("test@example.com");
        devUser.setName("Test Dev");

        ProjectEntity project = new ProjectEntity();
        project.setProjectId(projectId);
        project.setTitle("Test Project");

        // Save dependencies first
        userRepository.save(pmUser).block();
        userRepository.save(devUser).block();
        projectRepository.save(project).block();
    }

    /**
     * ******************
     * Positive Scenarios
     * ******************
     */
    @Test
    @Order(1)
    void createTicket_WithValidPayloadAndAuth_ShouldReturnOkAndStoreToDB() throws JOSEException {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act
        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .header("Authorization", "Bearer " + TestUtil.generateValidJwt(userId.toString()))
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("Idempotency-Key", UUID.randomUUID().toString())
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
                    assertEquals(TicketResponse.StatusEnum.OPEN.toString(), response.getStatus().toString());
                    ticketId = UUID.fromString(response.getTicketId());
                });

        System.out.println("Looking for ticket with ID: " + ticketId);//temp

        // Wait for async processing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert - Query real database
        StepVerifier.create(ticketRepository.findById(ticketId))
                .assertNext(savedTicket -> {
                    assertThat(savedTicket.getTicketId()).isEqualTo(ticketId);
                    assertThat(savedTicket.getSubject()).isEqualTo("Test Ticket");
                    assertThat(savedTicket.getDescription()).isEqualTo("This is a test ticket");
                    assertThat(savedTicket.getUserId()).isEqualTo(userId);
                    assertThat(savedTicket.getProjectId()).isEqualTo(projectId);
                    assertThat(savedTicket.getStatus()).isEqualTo(TicketResponse.StatusEnum.OPEN.toString());
                    assertThat(savedTicket.getCreatedAt()).isNotNull();
                    assertThat(savedTicket.getUpdatedAt()).isNull();
                    assertThat(savedTicket.getAssigneeId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @Order(2)
    void assignTicket_WithValidAuth_ShouldReturnOkAndStoreToDB() throws JOSEException {

        TicketAssignmentRequest assignRequest = new TicketAssignmentRequest();
        assignRequest.setAssigneeId(assigneeId.toString());

        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets/" + ticketId + "/assign")
                .header("Authorization", "Bearer " + TestUtil.generateValidJwt())
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(assignRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketAssignmentResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(ticketId.toString(), response.getTicketId());
                    assertNotNull(response.getAssignedAt());
                    assertNotNull(response.getAssignee());
                    assertEquals(assigneeId.toString(), response.getAssignee().getUserId());
                });

        // Wait for async processing by subscribers
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert - Query real database
        StepVerifier.create(ticketRepository.findById(ticketId))
                .assertNext(assignedTicket -> {
                    assertThat(assignedTicket.getAssigneeId()).isEqualTo(assigneeId);
                    assertThat(assignedTicket.getSubject()).isEqualTo("Test Ticket");
                    assertThat(assignedTicket.getDescription()).isEqualTo("This is a test ticket");
                    assertThat(assignedTicket.getStatus()).isEqualTo(TicketResponse.StatusEnum.OPEN.toString());
                    assertThat(assignedTicket.getUserId()).isEqualTo(userId);
                    assertThat(assignedTicket.getProjectId()).isEqualTo(projectId);
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    void updateTicketStatus_WithValidAuth_ShouldReturnOkAndStoreToDB() throws JOSEException {

        String expectedStatus = TicketResponse.StatusEnum.IN_PROGRESS.toString();

        TicketStatusRequest updateStatus = new TicketStatusRequest();
        updateStatus.setStatus(TicketStatusRequest.StatusEnum.fromValue(expectedStatus));

        webTestClient.patch()
                .uri("http://localhost:" + port + "/api/v1/tickets/" + ticketId + "/status")
                .header("Authorization", "Bearer " + TestUtil.generateValidJwt())
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updateStatus))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketStatusResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(ticketId.toString(), response.getTicketId());
                    assertEquals(expectedStatus, response.getStatus().toString());
                    assertNotNull(response.getUpdatedAt());
                });

        // Wait for async processing by subscribers
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert - Query real database
        StepVerifier.create(ticketRepository.findById(ticketId))
                .assertNext(updatedTicket -> {
                    assertThat(updatedTicket.getStatus()).isEqualToIgnoringCase(expectedStatus);
                    assertThat(updatedTicket.getAssigneeId()).isEqualTo(assigneeId);
                    assertThat(updatedTicket.getSubject()).isEqualTo("Test Ticket");
                    assertThat(updatedTicket.getDescription()).isEqualTo("This is a test ticket");
                    assertThat(updatedTicket.getUserId()).isEqualTo(userId);
                    assertThat(updatedTicket.getProjectId()).isEqualTo(projectId);
                })
                .verifyComplete();
    }
}
