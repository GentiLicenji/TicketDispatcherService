package com.pleased.ticket.dispatcher.server.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TicketCreated extends TicketEvent {
    private UUID ticketId;
    private String subject;
    private String description;
    private UUID userId;
    private UUID projectId;
    private OffsetDateTime createdAt;

    // Add this field
//    @JsonProperty("eventType")
//    private String eventType = "TicketCreated";
//
//    @JsonProperty("eventType")
//    public String getEventType() {
//        return "TicketCreated";
//    }
}




