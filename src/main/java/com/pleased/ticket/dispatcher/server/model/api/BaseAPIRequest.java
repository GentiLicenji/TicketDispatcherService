package com.pleased.ticket.dispatcher.server.model.api;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base API Request with common fields
 */
@Getter
@Setter
public abstract class BaseAPIRequest {
    private UUID correlationID;
    private UUID idempotencyKey;
    private String userAgent;
}






