package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.exception.EntityNotFoundException;
import com.pleased.ticket.dispatcher.server.model.api.TicketStatusEnum;
import com.pleased.ticket.dispatcher.server.model.dto.ProjectEntity;
import com.pleased.ticket.dispatcher.server.model.dto.TicketEntity;
import com.pleased.ticket.dispatcher.server.model.dto.UserEntity;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.repository.ProjectRepository;
import com.pleased.ticket.dispatcher.server.repository.TicketRepository;
import com.pleased.ticket.dispatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
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

    @BeforeAll
    static void setUp() {
        ticketId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
    }

    @Test
    void handleTicketCreated_ShouldCreateTicketInDatabase() {
        // Arrange - Create the required user first
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setEmail("test@example.com");
        user.setName("Test User");

        ProjectEntity project = new ProjectEntity();
        project.setProjectId(projectId);
        project.setTitle("Test Project");

        // Save dependencies first
        userRepository.save(user).block();
        projectRepository.save(project).block();

        // Arrange
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
        StepVerifier.create(ticketEventConsumer.handleTicketCreated(event, "ticket-create.v1", 0, 0))
                .verifyComplete();

        // Assert - Query real database
        StepVerifier.create(ticketRepository.findById(ticketId))
                .assertNext(savedTicket -> {
                    assertThat(savedTicket.getTicketId()).isEqualTo(ticketId);
                    assertThat(savedTicket.getSubject()).isEqualTo("Test Ticket");
                    assertThat(savedTicket.getDescription()).isEqualTo("This is a test ticket");
                    assertThat(savedTicket.getUserId()).isEqualTo(userId);
                    assertThat(savedTicket.getProjectId()).isEqualTo(projectId);
                    assertThat(savedTicket.getStatus()).isEqualTo(TicketStatusEnum.OPEN.toString());
                    assertThat(savedTicket.getCreatedAt()).isNotNull();
                    assertThat(savedTicket.getUpdatedAt()).isNull();
                    assertThat(savedTicket.getAssigneeId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void handleTicketAssigned_ShouldUpdateTicketInDatabase() {
        // Arrange - First create a ticket in the database
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        TicketEntity existingTicket = new TicketEntity();
        existingTicket.setTicketId(ticketId);
        existingTicket.setSubject("Test Ticket");
        existingTicket.setDescription("Test Description");
        existingTicket.setUserId(userId);
        existingTicket.setProjectId(projectId);
        existingTicket.setStatus(TicketStatusEnum.OPEN.toString());
        existingTicket.setCreatedAt(now.minusHours(1));

        // Save the initial ticket
        ticketRepository.save(existingTicket).block();

        TicketAssigned event = TicketAssigned.builder()
                .ticketId(ticketId)
                .assigneeId(assigneeId)
                .correlationId(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .assignedAt(now)
                .build();

        // Act
        StepVerifier.create(ticketEventConsumer.handleTicketAssigned(event, "ticket-assignments.v1", 0, 0))
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
        // Arrange - First create a ticket in the database
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        String newStatus = "CLOSED";
        OffsetDateTime now = OffsetDateTime.now();

        TicketEntity existingTicket = new TicketEntity();
        existingTicket.setTicketId(ticketId);
        existingTicket.setSubject("Test Ticket");
        existingTicket.setDescription("Test Description");
        existingTicket.setUserId(userId);
        existingTicket.setProjectId(projectId);
        existingTicket.setStatus(TicketStatusEnum.OPEN.toString());
        existingTicket.setCreatedAt(now.minusHours(1));

        // Save the initial ticket
        ticketRepository.save(existingTicket).block();

        TicketStatusUpdated event = TicketStatusUpdated.builder()
                .ticketId(ticketId)
                .status(newStatus)
                .correlationId(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .updatedAt(now)
                .build();

        // Act
        StepVerifier.create(ticketEventConsumer.handleTicketStatusUpdated(event, "ticket-updates.v1", 0, 0))
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

        TicketAssigned event = TicketAssigned.builder()
                .ticketId(ticketId)
                .assigneeId(assigneeId)
                .correlationId(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .assignedAt(OffsetDateTime.now())
                .build();

        // Act & Assert - Use StepVerifier to test reactive error handling
        StepVerifier.create(ticketEventConsumer.handleTicketAssigned(event, "ticket-assignments.v1", 0, 0))
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

        TicketStatusUpdated event = TicketStatusUpdated.builder()
                .ticketId(ticketId)
                .status(newStatus)
                .correlationId(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Act & Assert - Use StepVerifier to test reactive error handling
        StepVerifier.create(ticketEventConsumer.handleTicketStatusUpdated(event, "ticket-updates.v1", 0, 0))
                .expectErrorMatches(throwable ->
                        throwable instanceof EntityNotFoundException &&
                                throwable.getMessage().equals("Ticket not found: " + ticketId))
                .verify();

        // Verify that no ticket was created
        StepVerifier.create(ticketRepository.findById(ticketId))
                .verifyComplete();
    }
}