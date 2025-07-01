package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * API Request for ticket creation
 */
@Getter
@Setter
public class TicketCreateAPIRequest extends BaseAPIRequest {
    private String subject;
    private String description;
    private UUID userId;
    private UUID projectId;
}