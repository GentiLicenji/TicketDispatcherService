package com.pleased.ticket.dispatcher.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Implementing persistable since we are manually generating key on the client side.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("ticket")
public class TicketEntity implements Persistable<UUID> {

    @Id
    private UUID ticketId;

    @Transient
    private boolean isNew = true; // Mark as new by default

    private String subject;

    private String description;

    private String status;

    private Integer priority; // nullable

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private LocalDate dueDate;

    private UUID userId;       // FK to User
    private UUID assigneeId;   // FK to User
    private UUID projectId;    // FK to Project

    @Override
    public UUID getId() {
        return ticketId;
    }
}
