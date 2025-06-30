package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API Response for ticket status updates.
 */
@Getter
@Setter
public class TicketStatusAPIResponse extends BaseAPIResponse {
    private UUID ticketID;
    private String status;
    private OffsetDateTime updatedAt;
}