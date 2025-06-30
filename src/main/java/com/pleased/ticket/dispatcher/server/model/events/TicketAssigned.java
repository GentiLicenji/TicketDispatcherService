package com.pleased.ticket.dispatcher.server.model.events;

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
public class TicketAssigned extends TicketEvent {
    private UUID ticketId;
    private UUID assigneeId;
    private OffsetDateTime assignedAt;
}
