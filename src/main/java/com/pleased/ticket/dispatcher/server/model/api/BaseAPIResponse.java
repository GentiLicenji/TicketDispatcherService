package com.pleased.ticket.dispatcher.server.model.api;

import java.time.LocalDateTime;

/**
 * Base API Response with common fields
 */
public abstract class BaseAPIResponse {
    private String correlationID;
    private LocalDateTime timestamp;
    private String status;

    // Getters and setters
    public String getCorrelationID() {
        return correlationID;
    }

    public void setCorrelationID(String correlationID) {
        this.correlationID = correlationID;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

