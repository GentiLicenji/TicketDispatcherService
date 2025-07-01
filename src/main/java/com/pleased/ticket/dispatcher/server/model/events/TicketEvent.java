package com.pleased.ticket.dispatcher.server.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class TicketEvent {
    private UUID eventId;
    private UUID correlationId;
    private int eventVersion;  // For schema evolution
}
