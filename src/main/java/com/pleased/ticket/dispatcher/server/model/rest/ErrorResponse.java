package com.pleased.ticket.dispatcher.server.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ErrorResponse {
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("status")
    private Integer status;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("requestId")
    private String requestId;
    
    public ErrorResponse() {
        this.timestamp = Instant.now().toEpochMilli();
    }
    
    public ErrorResponse(String path, Integer status, String error, String message, String requestId) {
        this.timestamp = Instant.now().toEpochMilli();
        this.path = path;
        this.status = status;
        this.error = error;
        this.message = message;
        this.requestId = requestId;
    }
    
    // Fluent setters
    public ErrorResponse timestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public ErrorResponse path(String path) {
        this.path = path;
        return this;
    }
    
    public ErrorResponse status(Integer status) {
        this.status = status;
        return this;
    }
    
    public ErrorResponse error(String error) {
        this.error = error;
        return this;
    }
    
    public ErrorResponse message(String message) {
        this.message = message;
        return this;
    }
    
    public ErrorResponse requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
    
    // Getters and setters
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}