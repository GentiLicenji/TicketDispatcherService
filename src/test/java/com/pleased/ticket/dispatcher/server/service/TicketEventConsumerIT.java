//package com.pleased.ticket.dispatcher.server.service;
//
//import com.pleased.ticket.dispatcher.server.config.TestKafkaConfig;
//import com.pleased.ticket.dispatcher.server.exception.EntityNotFoundException;
//import com.pleased.ticket.dispatcher.server.model.api.TicketStatusEnum;
//import com.pleased.ticket.dispatcher.server.model.dto.ProjectEntity;
//import com.pleased.ticket.dispatcher.server.model.dto.TicketEntity;
//import com.pleased.ticket.dispatcher.server.model.dto.UserEntity;
//import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
//import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
//import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
//import com.pleased.ticket.dispatcher.server.repository.ProjectRepository;
//import com.pleased.ticket.dispatcher.server.repository.TicketRepository;
//import com.pleased.ticket.dispatcher.server.repository.UserRepository;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//import reactor.test.StepVerifier;
//
//import java.time.OffsetDateTime;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Integration tests for {@link TicketEventConsumer}.
// * <p>
// * Verifies the end-to-end behavior of ticket-related Kafka events, ensuring
// * that the database reflects the correct state after consuming:
// * <ul>
// *     <li>{@link com.pleased.ticket.dispatcher.server.model.events.TicketCreated}</li>
// *     <li>{@link com.pleased.ticket.dispatcher.server.model.events.TicketAssigned}</li>
// *     <li>{@link com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated}</li>
// * </ul>
// * <p>
// * Uses an in-memory H2 database with reactive repositories. Each test prepares
// * its own data and verifies persistence or failure scenarios using {@link StepVerifier}.
// * <p>
// * Scenarios covered:
// * <ul>
// *     <li>Ticket creation persists new ticket</li>
// *     <li>Ticket assignment updates assignee and timestamp</li>
// *     <li>Status update modifies the ticket state correctly</li>
// *     <li>Handles not-found cases by throwing {@link EntityNotFoundException}</li>
// * </ul>
// * <p>
// * Profile: {@code test}, DB is reset between each test run.
// */
//@SpringBootTest
//@Transactional
//@Disabled("To be manually triggered.Integration tests depend on a live Kafka instance (broker) to validate consumer behavior.")
//public class TicketEventConsumerIT {
//
//    @Autowired
//    private TicketRepository ticketRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private ProjectRepository projectRepository;
//
//    @Autowired
//    private TicketEventConsumer ticketEventConsumer;
//
//    private static UUID ticketId;
//    private static UUID userId;
//    private static UUID projectId;
//    private static UUID assigneeId;
//
//    @BeforeAll
//    static void setUp() {
//        ticketId = UUID.randomUUID();
//        userId = UUID.randomUUID();
//        projectId = UUID.randomUUID();
//        assigneeId = UUID.randomUUID();
//    }
//
//    /**
//     * Create the required user,assignee and project before each test run.
//     */
//    @BeforeEach
//    void setUpEach() {
//        //Clear db entries so we don't attempt double insertion.
//        ticketRepository.deleteAll().block();
//        userRepository.deleteAll().block();
//        projectRepository.deleteAll().block();
//
//        //Creating db entries for test setup
//        UserEntity pmUser = new UserEntity();
//        pmUser.setUserId(userId);
//        pmUser.setEmail("test@example.com");
//        pmUser.setName("Test PM");
//
//        UserEntity devUser = new UserEntity();
//        devUser.setUserId(assigneeId);
//        devUser.setEmail("test@example.com");
//        devUser.setName("Test Dev");
//
//        ProjectEntity project = new ProjectEntity();
//        project.setProjectId(projectId);
//        project.setTitle("Test Project");
//
//        // Save dependencies first
//        userRepository.save(pmUser).block();
//        userRepository.save(devUser).block();
//        projectRepository.save(project).block();
//
//        //Create existing ticket for updates,assignment
//        TicketEntity existingTicket = new TicketEntity();
//        existingTicket.setTicketId(ticketId);
//        existingTicket.setSubject("Test Ticket");
//        existingTicket.setDescription("Test Description");
//        existingTicket.setUserId(userId);
//        existingTicket.setProjectId(projectId);
//        existingTicket.setStatus(TicketStatusEnum.OPEN.toString());
//        existingTicket.setCreatedAt(OffsetDateTime.now());
//
//        // Save the initial ticket
//        ticketRepository.save(existingTicket).block();
//    }
//
//    /**
//     * Resets data for each test call.
//     * <p>
//     * Note: even though H2 is an in memory db, and it shouldn't persist data.
//     * Spring test stores the same context between all test calls.
//     */
//    @AfterEach
//    void tearDownEach() {
//        //Clear db entries so we don't attempt double insertion.
//        ticketRepository.deleteAll().block();
//        userRepository.deleteAll().block();
//        projectRepository.deleteAll().block();
//    }
//
//    @Test
//    void handleTicketCreated_ShouldCreateTicketInDatabase() {
//
//        // New ticket ID since it's a brand-new Ticket
//        UUID newTicket = UUID.randomUUID();
//
//        // Create a ticket event
//        TicketCreated event = TicketCreated.builder()
//                .ticketId(newTicket)
//                .subject("Test Ticket")
//                .description("This is a test ticket")
//                .userId(userId)
//                .projectId(projectId)
//                .correlationId(UUID.randomUUID())
//                .eventId(UUID.randomUUID())
//                .createdAt(OffsetDateTime.now())
//                .build();
//
//        // Act
//        StepVerifier.create(ticketEventConsumer.handleTicketCreated(event, "ticket-create.v1", 0, 0))
//                .verifyComplete();
//
//        // Assert - Query real database
//        StepVerifier.create(ticketRepository.findById(newTicket))
//                .assertNext(savedTicket -> {
//                    assertThat(savedTicket.getTicketId()).isEqualTo(newTicket);
//                    assertThat(savedTicket.getSubject()).isEqualTo("Test Ticket");
//                    assertThat(savedTicket.getDescription()).isEqualTo("This is a test ticket");
//                    assertThat(savedTicket.getUserId()).isEqualTo(userId);
//                    assertThat(savedTicket.getProjectId()).isEqualTo(projectId);
//                    assertThat(savedTicket.getStatus()).isEqualTo(TicketStatusEnum.OPEN.toString());
//                    assertThat(savedTicket.getCreatedAt()).isNotNull();
//                    assertThat(savedTicket.getUpdatedAt()).isNull();
//                    assertThat(savedTicket.getAssigneeId()).isNull();
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void handleTicketAssigned_ShouldUpdateTicketInDatabase() {
//
//        //Create assign-ticket event
//        OffsetDateTime now = OffsetDateTime.now();
//
//        TicketAssigned event = TicketAssigned.builder()
//                .ticketId(ticketId)
//                .assigneeId(assigneeId)
//                .correlationId(UUID.randomUUID())
//                .eventId(UUID.randomUUID())
//                .assignedAt(now)
//                .build();
//
//        ConsumerRecord<String, TicketAssigned> record= new ConsumerRecord<>()
//        // Act
//        StepVerifier.create(ticketEventConsumer.handleTicketAssigned())
//                .verifyComplete();
//
//        // Assert - Query real database
//        StepVerifier.create(ticketRepository.findById(ticketId))
//                .assertNext(updatedTicket -> {
//                    assertThat(updatedTicket.getTicketId()).isEqualTo(ticketId);
//                    assertThat(updatedTicket.getAssigneeId()).isEqualTo(assigneeId);
//                    assertThat(updatedTicket.getUpdatedAt()).isEqualTo(now);
//                    assertThat(updatedTicket.getSubject()).isEqualTo("Test Ticket");
//                    assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatusEnum.OPEN.toString());
//                    assertThat(updatedTicket.getUserId()).isEqualTo(userId);
//                    assertThat(updatedTicket.getProjectId()).isEqualTo(projectId);
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void handleTicketStatusUpdated_ShouldUpdateTicketStatusInDatabase() {
//        // Create update-status-ticket event
//        String newStatus = "CLOSED";
//        OffsetDateTime now = OffsetDateTime.now();
//
//        TicketStatusUpdated event = TicketStatusUpdated.builder()
//                .ticketId(ticketId)
//                .status(newStatus)
//                .correlationId(UUID.randomUUID())
//                .eventId(UUID.randomUUID())
//                .updatedAt(now)
//                .build();
//
//        // Act
//        StepVerifier.create(ticketEventConsumer.handleTicketStatusUpdated(event, "ticket-updates.v1", 0, 0))
//                .verifyComplete();
//
//        // Assert - Query real database
//        StepVerifier.create(ticketRepository.findById(ticketId))
//                .assertNext(updatedTicket -> {
//                    assertThat(updatedTicket.getTicketId()).isEqualTo(ticketId);
//                    assertThat(updatedTicket.getStatus()).isEqualTo(newStatus.toUpperCase());
//                    assertThat(updatedTicket.getUpdatedAt()).isEqualTo(now);
//                    assertThat(updatedTicket.getSubject()).isEqualTo("Test Ticket");
//                    assertThat(updatedTicket.getUserId()).isEqualTo(userId);
//                    assertThat(updatedTicket.getProjectId()).isEqualTo(projectId);
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void handleTicketAssigned_ShouldHandleTicketNotFound() {
//        // Arrange
//        UUID ticketId = UUID.randomUUID();
//        UUID assigneeId = UUID.randomUUID();
//
//        TicketAssigned event = TicketAssigned.builder()
//                .ticketId(ticketId)
//                .assigneeId(assigneeId)
//                .correlationId(UUID.randomUUID())
//                .eventId(UUID.randomUUID())
//                .assignedAt(OffsetDateTime.now())
//                .build();
//
//        // Act & Assert - Use StepVerifier to test reactive error handling
//        StepVerifier.create(ticketEventConsumer.handleTicketAssigned(event, "ticket-assignments.v1", 0, 0))
//                .expectErrorMatches(throwable ->
//                        throwable instanceof EntityNotFoundException &&
//                                throwable.getMessage().equals("Ticket not found: " + ticketId))
//                .verify();
//
//        // Verify that no ticket was created
//        StepVerifier.create(ticketRepository.findById(ticketId))
//                .verifyComplete();
//    }
//
//    @Test
//    void handleTicketStatusUpdated_ShouldHandleTicketNotFound() {
//        // Arrange
//        UUID ticketId = UUID.randomUUID();
//        String newStatus = "CLOSED";
//
//        TicketStatusUpdated event = TicketStatusUpdated.builder()
//                .ticketId(ticketId)
//                .status(newStatus)
//                .correlationId(UUID.randomUUID())
//                .eventId(UUID.randomUUID())
//                .updatedAt(OffsetDateTime.now())
//                .build();
//
//        // Act & Assert - Use StepVerifier to test reactive error handling
//        StepVerifier.create(ticketEventConsumer.handleTicketStatusUpdated(event, "ticket-updates.v1", 0, 0))
//                .expectErrorMatches(throwable ->
//                        throwable instanceof EntityNotFoundException &&
//                                throwable.getMessage().equals("Ticket not found: " + ticketId))
//                .verify();
//
//        // Verify that no ticket was created
//        StepVerifier.create(ticketRepository.findById(ticketId))
//                .verifyComplete();
//    }
//}