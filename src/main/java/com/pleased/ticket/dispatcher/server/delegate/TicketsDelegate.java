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
            String xCorrelationID,
            String idempotencyKey,
            String userAgent) {

        //Perform validation

        // Map REST request to API request
        TicketCreateAPIRequest apiRequest = ticketsMapper.fromRestToAPICreateRequest(restRequest);

        // Set additional context information
        apiRequest.setAuthorization(authorization);
        apiRequest.setCorrelationID(UUID.fromString(xCorrelationID));
        apiRequest.setIdempotencyKey(UUID.fromString(idempotencyKey));
        apiRequest.setUserAgent(userAgent);

        return ticketsApiService.createTicket(apiRequest)
                .map(ticketsMapper::fromAPIToRestTicketResponse);
    }

    public Mono<TicketAssignmentResponse> assignTicket(
            TicketAssignmentRequest restRequest,
            String authorization,
            String ticketID,
            String xCorrelationID,
            String idempotencyKey,
            String userAgent) {

        //Perform validation

        // Map REST request to API request
        TicketAssignmentAPIRequest apiRequest = ticketsMapper.fromRestToAPIAssignmentRequest(restRequest);

        // Set additional context information
        apiRequest.setTicketID(UUID.fromString(ticketID));
        apiRequest.setAuthorization(authorization);
        apiRequest.setCorrelationID(UUID.fromString(xCorrelationID));
        apiRequest.setIdempotencyKey(UUID.fromString(idempotencyKey));
        apiRequest.setUserAgent(userAgent);

        return ticketsApiService.assignTicket(apiRequest)
                .map(ticketsMapper::fromAPIToRestAssignmentResponse);
    }

//    public Mono<ResponseEntity<TicketResponse>> updateTicketDetails(
//            TicketDetailsRequest restRequest,
//            String authorization,
//            String ticketID,
//            String xCorrelationID,
//            String idempotencyKey,
//            String userAgent) {
//
//        return Mono.fromCallable(() -> {
//                    // Map REST request to API request
//                    TicketDetailsAPIRequest apiRequest = ticketsMapper.fromRestToAPIDetailsRequest(restRequest);
//
//                    // Set additional context information
//                    apiRequest.setTicketID(ticketID);
//                    apiRequest.setAuthorization(authorization);
//                    apiRequest.setCorrelationID(xCorrelationID);
//                    apiRequest.setIdempotencyKey(idempotencyKey);
//                    apiRequest.setUserAgent(userAgent);
//
//                    return apiRequest;
//                })
//                .flatMap(apiRequest -> ticketsApiService.updateTicketDetails(apiRequest))
//                .map(apiResponse -> {
//                    // Map API response back to REST response
//                    TicketResponse restResponse = ticketsMapper.fromAPIToRestTicketResponse(apiResponse);
//                    return new ResponseEntity<>(restResponse, HttpStatus.OK);
//                })
//                .onErrorMap(throwable -> {
//                    // Handle and wrap exceptions appropriately
//                    System.err.println("Error in updateTicketDetails: " + throwable.getMessage());
//                    return throwable;
//                });
//    }

    public Mono<TicketStatusResponse> updateTicketStatus(
            TicketStatusRequest restRequest,
            String authorization,
            String ticketID,
            String xCorrelationID,
            String idempotencyKey,
            String userAgent) {

        // Map REST request to API request
        TicketStatusAPIRequest apiRequest = ticketsMapper.fromRestToAPIStatusRequest(restRequest);

        // Set additional context information
        apiRequest.setTicketID(UUID.fromString(ticketID));
        apiRequest.setAuthorization(authorization);
        apiRequest.setCorrelationID(UUID.fromString(xCorrelationID));
        apiRequest.setIdempotencyKey(UUID.fromString(idempotencyKey));
        apiRequest.setUserAgent(userAgent);

        return ticketsApiService.updateTicketStatus(apiRequest)
                .map(ticketsMapper::fromAPIToRestStatusResponse);
    }
}