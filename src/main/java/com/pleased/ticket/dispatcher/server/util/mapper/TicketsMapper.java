package com.pleased.ticket.dispatcher.server.util.mapper;

import com.pleased.ticket.dispatcher.server.model.api.*;
import com.pleased.ticket.dispatcher.server.model.rest.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * Mapper interface for converting between REST and API models
 */
@Mapper(componentModel = "spring")
public interface TicketsMapper {

    // Assignment mapping methods
    @Mapping(source = "assigneeId", target = "assigneeId", qualifiedByName = "stringToUUID")
    TicketAssignmentAPIRequest fromRestToAPIAssignmentRequest(TicketAssignmentRequest restRequest);
    TicketAssignmentResponse fromAPIToRestAssignmentResponse(TicketAssignmentAPIResponse apiResponse);

    // Create mapping methods
    @Mapping(source = "projectId", target = "projectId", qualifiedByName = "stringToUUID")
    TicketCreateAPIRequest fromRestToAPICreateRequest(TicketCreateRequest restRequest);
    @Mapping(source = "projectId", target = "projectId", qualifiedByName = "uuidToString")
    TicketResponse fromAPIToRestTicketResponse(TicketAPIResponse apiResponse);

    // Status mapping methods
    TicketStatusAPIRequest fromRestToAPIStatusRequest(TicketStatusRequest restRequest);
    TicketStatusResponse fromAPIToRestStatusResponse(TicketStatusAPIResponse apiResponse);

    // Details mapping methods
    TicketDetailsAPIRequest fromRestToAPIDetailsRequest(TicketDetailsRequest restRequest);

    /**
     * Helper methods for custom mapping.
     */
    @Named("uuidToString")
    static String uuidToString(UUID id) {
        return id != null ? id.toString() : null;
    }
    @Named("stringToUUID")
    static UUID stringToUUID(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

}