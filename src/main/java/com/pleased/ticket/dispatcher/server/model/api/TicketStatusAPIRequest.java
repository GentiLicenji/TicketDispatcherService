package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * API Request for ticket status updates
 */
@Getter
@Setter
public class TicketStatusAPIRequest extends BaseAPIRequest {
    private UUID ticketID;
    private String status;
}

