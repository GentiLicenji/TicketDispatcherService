package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API Response for ticket operations (create, update details)
 */
@Getter
@Setter
public class TicketAPIResponse extends BaseAPIResponse {
    private UUID ticketID;
    private String subject;
    private String description;
    private String priority;
    private UUID projectId;
    private UUID userId;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
