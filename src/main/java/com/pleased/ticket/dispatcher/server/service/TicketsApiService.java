package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.model.api.*;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
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

    @Autowired
    public TicketsApiService(TicketEventProducer eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new ticket
     */
    public Mono<TicketAPIResponse> createTicket(TicketCreateAPIRequest request) {
        return Mono.fromCallable(() -> {
                    // Validate request
                    if (request.getSubject() == null || request.getIdempotencyKey() == null) {
                        throw new IllegalArgumentException("Title and IdempotencyKey are required");
                    }

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
                    return response;
                })
                .flatMap(response -> {
                    // Publish event
                    TicketCreated event = TicketCreated.builder()
                            .ticketId(response.getTicketID())
                            .subject(request.getSubject())
                            .description(request.getDescription())
                            .userId(request.getUserId())
                            .correlationId(request.getCorrelationID())
                            .eventId(request.getIdempotencyKey())
                            .build();

                    return eventPublisher.publishTicketCreated(event)
                            .then(Mono.just(response));
                })
                .doOnError(error -> log.error("Failed to create ticket with title: {}", request.getSubject(), error));
    }

    /**
     * Assign a ticket to a user.
     */
    public Mono<TicketAssignmentAPIResponse> assignTicket(TicketAssignmentAPIRequest request) {
        return Mono.fromCallable(() -> {
                    // Validate request
                    if (request.getTicketID() == null || request.getAssigneeId() == null) {
                        throw new IllegalArgumentException("TicketID and AssigneeId are required");
                    }

                    log.info("Assigning ticket {} to user {}", request.getTicketID(), request.getAssigneeId());

                    // Create and return fast response
                    TicketAssignmentAPIResponse response = new TicketAssignmentAPIResponse();
                    response.setTicketID(request.getTicketID());
                    response.setAssigneeId(request.getAssigneeId());
                    response.setAssignedAt(OffsetDateTime.now());
                    return response;
                })
                .flatMap(response -> {
                    // Publish event
                    TicketAssigned event = TicketAssigned.builder()
                            .ticketId(request.getTicketID())
                            .assigneeId(request.getAssigneeId())
                            .assignedAt(response.getAssignedAt())
                            .correlationId(request.getCorrelationID())
                            .eventId(request.getIdempotencyKey())
//                            .eventVersion(TicketEventProducer.eventVersion)TODO: supposed to handle multi schema
                            .build();

                    return eventPublisher.publishTicketAssigned(event)
                            .then(Mono.just(response));
                })
                .doOnError(error -> log.error("Failed to assign ticket {}", request.getTicketID(), error));
    }


    /**
     * Update ticket status
     */
    public Mono<TicketStatusAPIResponse> updateTicketStatus(TicketStatusAPIRequest request) {
        return Mono.fromCallable(() -> {
                    // Validate request
                    if (request.getTicketID() == null || request.getStatus() == null) {
                        throw new IllegalArgumentException("TicketID and Status are required");
                    }

                    log.info("Updating ticket {} status to: {}", request.getTicketID(), request.getStatus());

                    // Create and return fast response
                    TicketStatusAPIResponse response = new TicketStatusAPIResponse();
                    response.setTicketID(request.getTicketID());
                    response.setStatus(request.getStatus());
                    response.setUpdatedAt(OffsetDateTime.now());
                    return response;
                })
                .flatMap(response -> {
                    // Publish event
                    TicketStatusUpdated event = TicketStatusUpdated.builder()
                            .status(request.getStatus())
                            .ticketId(request.getTicketID())
                            .correlationId(request.getCorrelationID())
                            .eventId(request.getIdempotencyKey())
                            .build();

                    return eventPublisher.publishTicketStatusUpdated(event)
                            .then(Mono.just(response));
                })
                .doOnError(error -> log.error("Failed to update ticket {} status", request.getTicketID(), error));
    }
}
