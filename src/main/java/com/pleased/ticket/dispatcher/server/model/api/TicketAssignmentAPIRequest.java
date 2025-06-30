package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * API Request for ticket assignment
 */
@Getter
@Setter
public class TicketAssignmentAPIRequest extends BaseAPIRequest {
    private UUID ticketID;
    private UUID assigneeId;
}
