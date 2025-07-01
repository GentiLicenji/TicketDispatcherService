package com.pleased.ticket.dispatcher.server.repository;

import com.pleased.ticket.dispatcher.server.model.dto.TicketEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TicketRepository extends ReactiveCrudRepository<TicketEntity, UUID> {
    Flux<TicketEntity> findByProjectId(UUID projectId);
    Flux<TicketEntity> findByUserId(UUID userId);
}
