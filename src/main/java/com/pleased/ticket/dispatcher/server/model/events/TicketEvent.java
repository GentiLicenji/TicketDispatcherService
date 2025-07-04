package com.pleased.ticket.dispatcher.server.model.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Jackson annotations for polymorphic deserialization by a single consumer factory
 */
//@JsonTypeInfo(
//        use = JsonTypeInfo.Id.NAME,
//        include = JsonTypeInfo.As.PROPERTY,
//        property = "eventType"
//)
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = TicketCreated.class, name = "TicketCreated"),
//        @JsonSubTypes.Type(value = TicketAssigned.class, name = "TicketAssigned"),
//        @JsonSubTypes.Type(value = TicketStatusUpdated.class, name = "TicketStatusUpdated")
//})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class TicketEvent {
    private UUID eventId;
    private UUID correlationId;
    private int eventVersion;  // For schema evolution
}
