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
public class TicketStatusUpdated extends TicketEvent {
    private UUID ticketId;
    private String status;
    private OffsetDateTime updatedAt;

//    @JsonProperty("eventType")
//    private String eventType = "TicketStatusUpdated";

    @JsonProperty("eventType")
    public String getEventType() {
        return "TicketStatusUpdated";
    }
}
