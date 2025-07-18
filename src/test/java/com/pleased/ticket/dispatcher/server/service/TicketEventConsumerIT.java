package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.config.KafkaTopicConfig;
import com.pleased.ticket.dispatcher.server.exception.EntityNotFoundException;
import com.pleased.ticket.dispatcher.server.model.api.TicketStatusEnum;
import com.pleased.ticket.dispatcher.server.model.dto.ProjectEntity;
import com.pleased.ticket.dispatcher.server.model.dto.TicketEntity;
import com.pleased.ticket.dispatcher.server.model.dto.UserEntity;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.model.rest.TicketResponse;
import com.pleased.ticket.dispatcher.server.repository.ProjectRepository;
import com.pleased.ticket.dispatcher.server.repository.TicketRepository;
import com.pleased.ticket.dispatcher.server.repository.UserRepository;
import com.pleased.ticket.dispatcher.server.util.mapper.UUIDConverter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TicketEventConsumer}.
 * <p>
 * Verifies the end-to-end behavior of ticket-related Kafka events, ensuring
 * that the database reflects the correct state after consuming:
 * <ul>
 *     <li>{@link com.pleased.ticket.dispatcher.server.model.events.TicketCreated}</li>
 *     <li>{@link com.pleased.ticket.dispatcher.server.model.events.TicketAssigned}</li>
 *     <li>{@link com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated}</li>
 * </ul>
 * <p>
 * Uses an in-memory H2 database with reactive repositories. All tests have the same
 * data setup and verifies persistence or failure scenarios using {@link StepVerifier}.
 * <p>
 * Scenarios covered:
 * <ul>
 *     <li>Ticket creation persists new ticket</li>
 *     <li>Ticket assignment updates assignee and timestamp</li>
 *     <li>Status update modifies the ticket state correctly</li>
 *     <li>Handles not-found cases by throwing {@link EntityNotFoundException}</li>
 * </ul>
 * <p>
 * Profile: {@code test}
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TicketEventConsumerIT {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TicketEventConsumer ticketEventConsumer;

    private static UUID ticketId;
    private static UUID userId;
    private static UUID projectId;
    private static UUID assigneeId;

    @BeforeAll
    static void setUp() {
        ticketId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
    }

    /**
     * Create the required user,assignee and project before each test run.
     */
    @BeforeAll
    void setUpEach() {
        //Clear db entries so we don't attempt double insertion.
        ticketRepository.deleteAll().block();
        userRepository.deleteAll().block();
        projectRepository.deleteAll().block();

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

        //Create existing ticket for updates,assignment
        TicketEntity existingTicket = new TicketEntity();
        existingTicket.setTicketId(ticketId);
        existingTicket.setSubject("Test Ticket");
        existingTicket.setDescription("Test Description");
        existingTicket.setUserId(userId);
        existingTicket.setProjectId(projectId);
        existingTicket.setStatus(TicketStatusEnum.OPEN.toString());
        existingTicket.setCreatedAt(OffsetDateTime.now());

        // Save the initial ticket
        ticketRepository.save(existingTicket).block();
    }

    @Test
    void handleTicketCreated_ShouldCreateTicketInDatabase() {

        // New ticket ID since it's a brand-new Ticket
        UUID newTicket = UUID.randomUUID();

        // Create a ticket event
        TicketCreated event = TicketCreated.newBuilder()
                .setTicketId(UUIDConverter.uuidToBytes(newTicket))
                .setSubject("Test Ticket")
                .setDescription("This is a test ticket")
                .setUserId(UUIDConverter.uuidToBytes(userId))
                .setProjectId(UUIDConverter.uuidToBytes(projectId))
                .setEventId(UUIDConverter.uuidToBytes(UUID.randomUUID()))
                .setCreatedAt(OffsetDateTime.now().toInstant())
                .build();

        ConsumerRecord<ByteBuffer, TicketCreated> record = new ConsumerRecord<>(
                KafkaTopicConfig.TICKET_CREATE_TOPIC,
                0,
                0L,
                UUIDConverter.uuidToBytes(newTicket),
                event
        );
        // Act
        StepVerifier.create(ticketEventConsumer.handleTicketCreated(record))
                .verifyComplete();

        // Assert - Query real database
        StepVerifier.create(ticketRepository.findById(newTicket))
                .assertNext(savedTicket -> {
                    assertThat(savedTicket.getTicketId()).isEqualTo(newTicket);
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
    void handleTicketAssigned_ShouldUpdateTicketInDatabase() {

        //Create assign-ticket event
        OffsetDateTime now = OffsetDateTime.now();

        TicketAssigned event = TicketAssigned.newBuilder()
                .setTicketId(UUIDConverter.uuidToBytes(ticketId))
                .setAssigneeId(UUIDConverter.uuidToBytes(assigneeId))
                .setEventId(UUIDConverter.uuidToBytes(UUID.randomUUID()))
                .setAssignedAt(now.toInstant())
                .build();

        ConsumerRecord<ByteBuffer, TicketAssigned> record = new ConsumerRecord<>(
                KafkaTopicConfig.TICKET_ASSIGNMENTS_TOPIC,
                0,
                0L,
                UUIDConverter.uuidToBytes(ticketId),
                event
        );
        // Act
        StepVerifier.create(ticketEventConsumer.handleTicketAssigned(record))
                .verifyComplete();

        // Assert - Query real database
        StepVerifier.create(ticketRepository.findById(ticketId))
                .assertNext(updatedTicket -> {
                    assertThat(updatedTicket.getTicketId()).isEqualTo(ticketId);
                    assertThat(updatedTicket.getAssigneeId()).isEqualTo(assigneeId);
                    assertThat(updatedTicket.getUpdatedAt()).isEqualTo(now);
                    assertThat(updatedTicket.getSubject()).isEqualTo("Test Ticket");
                    assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatusEnum.OPEN.toString());
                    assertThat(updatedTicket.getUserId()).isEqualTo(userId);
                    assertThat(updatedTicket.getProjectId()).isEqualTo(projectId);
                })
                .verifyComplete();
    }

    @Test
    void handleTicketStatusUpdated_ShouldUpdateTicketStatusInDatabase() {
        // Create update-status-ticket event
        String newStatus = "CLOSED";
        OffsetDateTime now = OffsetDateTime.now();

        TicketStatusUpdated event = TicketStatusUpdated.newBuilder()
                .setTicketId(UUIDConverter.uuidToBytes(ticketId))
                .setStatus(newStatus)
                .setEventId(UUIDConverter.uuidToBytes(UUID.randomUUID()))
                .setUpdatedAt(now.toInstant())
                .build();

        ConsumerRecord<ByteBuffer, TicketStatusUpdated> record = new ConsumerRecord<>(
                KafkaTopicConfig.TICKET_UPDATES_TOPIC,
                0,
                0L,
                UUIDConverter.uuidToBytes(ticketId),
                event
        );
        // Act
        StepVerifier.create(ticketEventConsumer.handleTicketStatusUpdated(record))
                .verifyComplete();

        // Assert - Query real database
        StepVerifier.create(ticketRepository.findById(ticketId))
                .assertNext(updatedTicket -> {
                    assertThat(updatedTicket.getTicketId()).isEqualTo(ticketId);
                    assertThat(updatedTicket.getStatus()).isEqualTo(newStatus.toUpperCase());
                    assertThat(updatedTicket.getUpdatedAt()).isEqualTo(now);
                    assertThat(updatedTicket.getSubject()).isEqualTo("Test Ticket");
                    assertThat(updatedTicket.getUserId()).isEqualTo(userId);
                    assertThat(updatedTicket.getProjectId()).isEqualTo(projectId);
                })
                .verifyComplete();
    }

    @Test
    void handleTicketAssigned_ShouldHandleTicketNotFound() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        TicketAssigned event = TicketAssigned.newBuilder()
                .setTicketId(UUIDConverter.uuidToBytes(ticketId))
                .setAssigneeId(UUIDConverter.uuidToBytes(assigneeId))
                .setEventId(UUIDConverter.uuidToBytes(UUID.randomUUID()))
                .setAssignedAt(OffsetDateTime.now().toInstant())
                .build();
        ConsumerRecord<ByteBuffer, TicketAssigned> record = new ConsumerRecord<>(
                KafkaTopicConfig.TICKET_ASSIGNMENTS_TOPIC,
                0,
                0L,
                UUIDConverter.uuidToBytes(ticketId),
                event
        );

        // Act & Assert - Use StepVerifier to test reactive error handling
        StepVerifier.create(ticketEventConsumer.handleTicketAssigned(record))
                .expectErrorMatches(throwable ->
                        throwable instanceof EntityNotFoundException &&
                                throwable.getMessage().equals("Ticket not found: " + ticketId))
                .verify();

        // Verify that no ticket was created
        StepVerifier.create(ticketRepository.findById(ticketId))
                .verifyComplete();
    }

    @Test
    void handleTicketStatusUpdated_ShouldHandleTicketNotFound() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        String newStatus = "CLOSED";

        TicketStatusUpdated event = TicketStatusUpdated.newBuilder()
                .setTicketId(UUIDConverter.uuidToBytes(ticketId))
                .setStatus(newStatus)
                .setEventId(UUIDConverter.uuidToBytes(UUID.randomUUID()))
                .setUpdatedAt(OffsetDateTime.now().toInstant())
                .build();
        ConsumerRecord<ByteBuffer, TicketStatusUpdated> record = new ConsumerRecord<>(
                KafkaTopicConfig.TICKET_UPDATES_TOPIC,
                0,
                0L,
                UUIDConverter.uuidToBytes(ticketId),
                event
        );

        // Act & Assert - Use StepVerifier to test reactive error handling
        StepVerifier.create(ticketEventConsumer.handleTicketStatusUpdated(record))
                .expectErrorMatches(throwable ->
                        throwable instanceof EntityNotFoundException &&
                                throwable.getMessage().equals("Ticket not found: " + ticketId))
                .verify();

        // Verify that no ticket was created
        StepVerifier.create(ticketRepository.findById(ticketId))
                .verifyComplete();
    }
}