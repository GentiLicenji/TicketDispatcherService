package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API Response for ticket assignment
 */
@Getter
@Setter
//TODO:needs removing name and role.
public class TicketAssignmentAPIResponse extends BaseAPIResponse {
    private UUID ticketID;
    private UUID assigneeId;
    private String assigneeName;// Note: we can't set name and role because it would make the api call blocking.
    private String assigneeRole;
    private OffsetDateTime assignedAt;
}


