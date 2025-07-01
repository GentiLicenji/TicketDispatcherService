package com.pleased.ticket.dispatcher.server.repository;

import com.pleased.ticket.dispatcher.server.model.dto.ProjectEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ProjectRepository extends ReactiveCrudRepository<ProjectEntity, UUID> {
    Flux<ProjectEntity> findByProjectOwnerId(UUID userId);
}
