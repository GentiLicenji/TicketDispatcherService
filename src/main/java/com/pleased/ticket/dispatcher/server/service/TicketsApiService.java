package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.model.api.*;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.util.mapper.EventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ticket Service is responsible for publishing rest requests to Kafka topics.
 * <p>
 * Service will:
 * Map API requests to Kafka Events
 * Publish Events to the following kafka topics: ticket-create.v1 ; ticket-assignments.v1; ticket-updates.v1;
 */
@Slf4j
@Service
public class TicketsApiService {

    private final TicketEventProducer eventPublisher;
    private final EventMapper eventMapper;

    @Autowired
    public TicketsApiService(TicketEventProducer eventPublisher, EventMapper eventMapper) {
        this.eventPublisher = eventPublisher;
        this.eventMapper = eventMapper;
    }

    /**
     * Create a new ticket
     */
    public Mono<TicketAPIResponse> createTicket(TicketCreateAPIRequest request) {

        UUID ticketId = request.getIdempotencyKey();
        log.info("Creating ticket {} with title: {}", ticketId, request.getSubject());

        // Create and return fast response
        TicketAPIResponse response = new TicketAPIResponse();
        response.setTicketID(ticketId);
        response.setSubject(request.getSubject());
        response.setDescription(request.getDescription());
        response.setUserId(request.getUserId());
        response.setProjectId(request.getProjectId());
        response.setStatus(TicketStatusEnum.OPEN.toString());
        response.setCreatedAt(OffsetDateTime.now());

        TicketCreated event= eventMapper.toTicketCreated(request, response.getTicketID(),response.getCreatedAt().toInstant());

        return eventPublisher.publishTicketCreated(event,request.getCorrelationID())
                .doOnError(error -> log.error("Failed to create ticket with title: {}", request.getSubject(), error))
                .then(Mono.just(response));
    }

    /**
     * Assign a ticket to a user.
     */
    public Mono<TicketAssignmentAPIResponse> assignTicket(TicketAssignmentAPIRequest request) {

        log.info("Assigning ticket {} to user {}", request.getTicketID(), request.getAssigneeId());

        // Create and return fast response
        TicketAssignmentAPIResponse response = new TicketAssignmentAPIResponse();
        response.setTicketID(request.getTicketID());
        response.setAssigneeId(request.getAssigneeId());
        response.setAssignedAt(OffsetDateTime.now());

        TicketAssigned event = eventMapper.toTicketAssigned(request, response.getAssignedAt().toInstant());


        return eventPublisher.publishTicketAssigned(event,request.getCorrelationID())
                .doOnError(error -> log.error("Failed to assign ticket {}", request.getTicketID(), error))
                .then(Mono.just(response));
    }

    /**
     * Update ticket status
     */
    public Mono<TicketStatusAPIResponse> updateTicketStatus(TicketStatusAPIRequest request) {

        log.info("Updating ticket {} status to: {}", request.getTicketID(), request.getStatus());

        // Create and return fast response
        TicketStatusAPIResponse response = new TicketStatusAPIResponse();
        response.setTicketID(request.getTicketID());
        response.setStatus(request.getStatus());
        response.setUpdatedAt(OffsetDateTime.now());

        TicketStatusUpdated event = eventMapper.toTicketStatusUpdated(request,response.getUpdatedAt().toInstant());

        return eventPublisher.publishTicketStatusUpdated(event,request.getCorrelationID())
                .doOnError(error -> log.error("Failed to update ticket {} status", request.getTicketID(), error))
                .then(Mono.just(response));
    }
}
