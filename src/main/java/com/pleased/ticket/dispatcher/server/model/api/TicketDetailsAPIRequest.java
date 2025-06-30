package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

/**
 * API Request for ticket details update
 */
@Getter
@Setter
public class TicketDetailsAPIRequest extends BaseAPIRequest {
    private String ticketID;
    private String title;
    private String description;
    private String priority;
    private String category;
}
