package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.model.api.*;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.util.mapper.EventMapperImpl;
import com.pleased.ticket.dispatcher.server.util.mapper.UUIDConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TicketsApiService.
 * <p>
 * Mocks all external components and checks for code invocation.
 */
@ExtendWith(MockitoExtension.class)
@Import({EventMapperImpl.class}) // import the generated impl class
public class TicketsApiServiceTest {

    @Mock
    private TicketEventProducer eventProducer;

    @Captor
    private ArgumentCaptor<TicketCreated> ticketCreatedCaptor;

    @Captor
    private ArgumentCaptor<TicketAssigned> ticketAssignedCaptor;

    @Captor
    private ArgumentCaptor<TicketStatusUpdated> ticketStatusUpdatedCaptor;

    private TicketsApiService ticketsApiService;

    @BeforeEach
    void setUp() {
        EventMapperImpl eventMapper = new EventMapperImpl();
        ticketsApiService = new TicketsApiService(eventProducer, eventMapper);

        // Setup default behavior for mocks with lenient to allow unused stubbings
        lenient().when(eventProducer.publishTicketCreated(any(), any())).thenReturn(Mono.empty());
        lenient().when(eventProducer.publishTicketAssigned(any(), any())).thenReturn(Mono.empty());
        lenient().when(eventProducer.publishTicketStatusUpdated(any(), any())).thenReturn(Mono.empty());
    }

    @Test
    void createTicket_ShouldCreateTicketAndPublishEvent() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        TicketCreateAPIRequest request = new TicketCreateAPIRequest();
        request.setIdempotencyKey(ticketId);
        request.setCorrelationID(correlationId);
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setUserId(UUID.randomUUID());
        request.setProjectId(UUID.randomUUID());

        // Act
        TicketAPIResponse response = ticketsApiService.createTicket(request).block(Duration.ofSeconds(5));

        // Assert
        assertNotNull(response);
        assertEquals(ticketId, response.getTicketID());
        assertEquals(request.getSubject(), response.getSubject());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getUserId(), response.getUserId());
        assertEquals(request.getProjectId(), response.getProjectId());
        assertEquals(TicketStatusEnum.OPEN.toString(), response.getStatus());
        assertNotNull(response.getCreatedAt());

        verify(eventProducer).publishTicketCreated(ticketCreatedCaptor.capture(), eq(correlationId));
        TicketCreated capturedEvent = ticketCreatedCaptor.getValue();
        assertEquals(ticketId, UUIDConverter.bytesToUUID(capturedEvent.getTicketId()));
        assertEquals(request.getSubject(), capturedEvent.getSubject());
        assertEquals(request.getDescription(), capturedEvent.getDescription());
        assertEquals(request.getUserId(), UUIDConverter.bytesToUUID(capturedEvent.getUserId()));
        assertEquals(ticketId, UUIDConverter.bytesToUUID(capturedEvent.getEventId()));
        assertNotNull(capturedEvent.getCreatedAt());
    }

    @Test
    void assignTicket_ShouldAssignTicketAndPublishEvent() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        TicketAssignmentAPIRequest request = new TicketAssignmentAPIRequest();
        request.setTicketID(ticketId);
        request.setAssigneeId(assigneeId);
        request.setCorrelationID(correlationId);
        request.setIdempotencyKey(idempotencyKey);

        // Act
        TicketAssignmentAPIResponse response = ticketsApiService.assignTicket(request).block(Duration.ofSeconds(5));

        // Assert
        assertNotNull(response);
        assertEquals(ticketId, response.getTicketID());
        assertEquals(assigneeId, response.getAssigneeId());
        assertNotNull(response.getAssignedAt());

        verify(eventProducer).publishTicketAssigned(ticketAssignedCaptor.capture(),  eq(correlationId));
        TicketAssigned capturedEvent = ticketAssignedCaptor.getValue();
        assertEquals(ticketId,UUIDConverter.bytesToUUID( capturedEvent.getTicketId()));
        assertEquals(assigneeId, UUIDConverter.bytesToUUID(capturedEvent.getAssigneeId()));
        assertEquals(idempotencyKey, UUIDConverter.bytesToUUID(capturedEvent.getEventId()));
        assertNotNull(capturedEvent.getAssignedAt());
    }

    @Test
    void updateTicketStatus_ShouldUpdateStatusAndPublishEvent() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        String newStatus = "CLOSED";

        TicketStatusAPIRequest request = new TicketStatusAPIRequest();
        request.setTicketID(ticketId);
        request.setStatus(newStatus);
        request.setCorrelationID(correlationId);
        request.setIdempotencyKey(idempotencyKey);

        // Act
        TicketStatusAPIResponse response = ticketsApiService.updateTicketStatus(request).block(Duration.ofSeconds(5));

        // Assert
        assertNotNull(response);
        assertEquals(ticketId, response.getTicketID());
        assertEquals(newStatus, response.getStatus());
        assertNotNull(response.getUpdatedAt());

        verify(eventProducer).publishTicketStatusUpdated(ticketStatusUpdatedCaptor.capture(), eq(correlationId));
        TicketStatusUpdated capturedEvent = ticketStatusUpdatedCaptor.getValue();
        assertEquals(ticketId, UUIDConverter.bytesToUUID(capturedEvent.getTicketId()));
        assertEquals(newStatus, capturedEvent.getStatus());
        assertEquals(idempotencyKey,UUIDConverter.bytesToUUID( capturedEvent.getEventId()));
        assertNotNull(capturedEvent.getUpdatedAt());
    }

    @Test
    void createTicket_ShouldPropagateErrorFromEventProducer() {
        // Arrange
        UUID ticketId = UUID.randomUUID();

        TicketCreateAPIRequest request = new TicketCreateAPIRequest();
        request.setIdempotencyKey(ticketId);
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setUserId(UUID.randomUUID());
        request.setProjectId(UUID.randomUUID());

        // Setup mock to return error
        when(eventProducer.publishTicketCreated(any(), any())).thenReturn(Mono.error(new RuntimeException("Test error")));

        // Act & Assert - should propagate the error
        assertThrows(RuntimeException.class, () -> {
            ticketsApiService.createTicket(request).block(Duration.ofSeconds(5));
        });

        verify(eventProducer).publishTicketCreated(any(), any());
    }
}
