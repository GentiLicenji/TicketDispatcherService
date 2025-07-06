package com.pleased.ticket.dispatcher.server.util.mapper;


import com.pleased.ticket.dispatcher.server.model.api.TicketAssignmentAPIRequest;
import com.pleased.ticket.dispatcher.server.model.api.TicketCreateAPIRequest;
import com.pleased.ticket.dispatcher.server.model.api.TicketStatusAPIRequest;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import org.mapstruct.*;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE //Ignoring mapping of null fields to avoid NPE
)
@Mapper(componentModel = "spring")
public interface EventMapper {

    // TicketCreated mapping
    @Mapping(target = "eventId", source = "request.idempotencyKey", qualifiedByName = "uuidToBytes")
    @Mapping(target = "eventType", constant = "TicketCreated")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "ticketId", source = "ticketId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "subject", source = "request.subject")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "userId", source = "request.userId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "projectId", source = "request.projectId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    TicketCreated toTicketCreated(TicketCreateAPIRequest request, UUID ticketId);

    // Alternative with separate createdAt parameter
    @Mapping(target = "eventId", source = "request.idempotencyKey", qualifiedByName = "uuidToBytes")
    @Mapping(target = "eventType", constant = "TicketCreated")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "ticketId", source = "ticketId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "subject", source = "request.subject")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "userId", source = "request.userId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "projectId", source = "request.projectId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "createdAt", source = "createdAt")
    void updateTicketCreated(TicketCreateAPIRequest request, UUID ticketId, Instant createdAt, @MappingTarget TicketCreated target);

    // TicketStatusUpdated mapping
    @Mapping(target = "eventId", source = "request.idempotencyKey", qualifiedByName = "uuidToBytes")
    @Mapping(target = "eventType", constant = "TicketStatusUpdated")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "ticketId", source = "request.ticketID", qualifiedByName = "uuidToBytes")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    TicketStatusUpdated toTicketStatusUpdated(TicketStatusAPIRequest request);

    // Alternative with separate updatedAt parameter
    @Mapping(target = "eventId", source = "request.idempotencyKey", qualifiedByName = "uuidToBytes")
    @Mapping(target = "eventType", constant = "TicketStatusUpdated")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "ticketId", source = "request.ticketID", qualifiedByName = "uuidToBytes")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "updatedAt", source = "updatedAt")
    TicketStatusUpdated toTicketStatusUpdated(TicketStatusAPIRequest request, Instant updatedAt);

    // TicketAssigned mapping
    @Mapping(target = "eventId", source = "request.idempotencyKey", qualifiedByName = "uuidToBytes")
    @Mapping(target = "eventType", constant = "TicketAssigned")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "ticketId", source = "request.ticketID", qualifiedByName = "uuidToBytes")
    @Mapping(target = "assigneeId", source = "request.assigneeId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "assignedAt", expression = "java(java.time.Instant.now())")
    TicketAssigned toTicketAssigned(TicketAssignmentAPIRequest request);

    // Alternative with separate assignedAt parameter
    @Mapping(target = "eventId", source = "request.idempotencyKey", qualifiedByName = "uuidToBytes")
    @Mapping(target = "eventType", constant = "TicketAssigned")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "ticketId", source = "request.ticketID", qualifiedByName = "uuidToBytes")
    @Mapping(target = "assigneeId", source = "request.assigneeId", qualifiedByName = "uuidToBytes")
    @Mapping(target = "assignedAt", source = "assignedAt")
    TicketAssigned toTicketAssigned(TicketAssignmentAPIRequest request, Instant assignedAt);

    // UUID to ByteBuffer conversion
    @Named("uuidToBytes")
    default ByteBuffer uuidToBytes(UUID uuid) {
        return UUIDConverter.uuidToBytes(uuid);
    }

    // ByteBuffer to UUID conversion (for reverse mapping if needed)
    @Named("bytesToUuid")
    default UUID bytesToUuid(ByteBuffer buffer) {
        return UUIDConverter.bytesToUUID(buffer);
    }
}
