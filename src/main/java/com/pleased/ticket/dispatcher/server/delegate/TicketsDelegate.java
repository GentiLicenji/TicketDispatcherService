package com.pleased.ticket.dispatcher.server.delegate;

import com.pleased.ticket.dispatcher.server.model.api.TicketAssignmentAPIRequest;
import com.pleased.ticket.dispatcher.server.model.api.TicketCreateAPIRequest;
import com.pleased.ticket.dispatcher.server.model.api.TicketStatusAPIRequest;
import com.pleased.ticket.dispatcher.server.model.rest.*;
import com.pleased.ticket.dispatcher.server.service.TicketsApiService;
import com.pleased.ticket.dispatcher.server.util.mapper.TicketsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
public class TicketsDelegate {

    private final TicketsApiService ticketsApiService;
    private final TicketsMapper ticketsMapper;

    @Autowired
    public TicketsDelegate(TicketsApiService ticketsApiService, TicketsMapper ticketsMapper) {
        this.ticketsApiService = ticketsApiService;
        this.ticketsMapper = ticketsMapper;
    }

    public Mono<TicketResponse> createTicket(
            TicketCreateRequest restRequest,
            String authorization,
            UUID xCorrelationID,
            UUID idempotencyKey,
            String userAgent) {


        // Map REST request to API request
        TicketCreateAPIRequest apiRequest = ticketsMapper.fromRestToAPICreateRequest(restRequest);

        //TODO: Derive userID from jwt sub field by decoding auth header.
//        Ideally should be derived from the authPrinciple in a security filter.
//        apiRequest.setUserId(extractUserID(authorization));

        // Set additional context information
        apiRequest.setCorrelationID(xCorrelationID);
        apiRequest.setIdempotencyKey(idempotencyKey);
        apiRequest.setUserAgent(userAgent);

        return ticketsApiService.createTicket(apiRequest)
                .map(ticketsMapper::fromAPIToRestTicketResponse);
    }

    public Mono<TicketAssignmentResponse> assignTicket(
            TicketAssignmentRequest restRequest,
            String ticketID,
            String xCorrelationID,
            String idempotencyKey,
            String userAgent) {

        // Map REST request to API request
        TicketAssignmentAPIRequest apiRequest = ticketsMapper.fromRestToAPIAssignmentRequest(restRequest);

        // Set additional context information
        apiRequest.setTicketID(UUID.fromString(ticketID));
        apiRequest.setCorrelationID(UUID.fromString(xCorrelationID));
        apiRequest.setIdempotencyKey(UUID.fromString(idempotencyKey));
        apiRequest.setUserAgent(userAgent);

        return ticketsApiService.assignTicket(apiRequest)
                .map(ticketsMapper::fromAPIToRestAssignmentResponse);
    }

    public Mono<TicketResponse> updateTicketDetails(
            TicketDetailsRequest restRequest,
            String ticketID,
            String xCorrelationID,
            String idempotencyKey,
            String userAgent) {

        //TODO: out of scope and time.
        // Simulating this part
        return Mono.delay(Duration.ofMillis(100))
                .then(Mono.fromCallable(() -> {

                    TicketResponse response = new TicketResponse();
                    response.setTicketId(ticketID);
                    response.setStatus(TicketResponse.StatusEnum.IN_PROGRESS);

                    if (restRequest != null) {
                        response.setDescription(restRequest.getDescription());
                    }

                    return response;
                }));
    }

    public Mono<TicketStatusResponse> updateTicketStatus(
            TicketStatusRequest restRequest,
            String ticketID,
            String xCorrelationID,
            String idempotencyKey,
            String userAgent) {

        // Map REST request to API request
        TicketStatusAPIRequest apiRequest = ticketsMapper.fromRestToAPIStatusRequest(restRequest);

        // Set additional context information
        apiRequest.setTicketID(UUID.fromString(ticketID));
        apiRequest.setCorrelationID(UUID.fromString(xCorrelationID));
        apiRequest.setIdempotencyKey(UUID.fromString(idempotencyKey));
        apiRequest.setUserAgent(userAgent);

        return ticketsApiService.updateTicketStatus(apiRequest)
                .map(ticketsMapper::fromAPIToRestStatusResponse);
    }
}