package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.exception.EntityNotFoundException;
import com.pleased.ticket.dispatcher.server.model.api.TicketStatusEnum;
import com.pleased.ticket.dispatcher.server.model.dto.TicketEntity;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@Slf4j
@KafkaListener(topics = {"ticket-create.v1", "ticket-assignments.v1", "ticket-updates.v1"})
public class TicketEventConsumer {

    private final TicketRepository ticketRepository;

    @Autowired
    public TicketEventConsumer(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @KafkaListener(
            topics = "ticket-create.v1",
            groupId = "ticket-service-create-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public Mono<Void> handleTicketCreated(
            @Payload TicketCreated event,
            @Header("kafka_receivedTopic") String topic,
            @Header("kafka_receivedPartition") int partition,
            @Header("kafka_offset") long offset) {

        log.info("Processing TicketCreated event: ticketId={}, topic={}, partition={}, offset={}",
                event.getTicketId(), topic, partition, offset);

        return Mono.fromCallable(() -> {
                    TicketEntity entity = new TicketEntity();
                    entity.setTicketId(event.getTicketId());
                    entity.setSubject(event.getSubject());
                    entity.setDescription(event.getDescription());
                    entity.setStatus(TicketStatusEnum.OPEN.toString()); // Default status for new tickets
                    entity.setCreatedAt(OffsetDateTime.now());
                    entity.setUserId(event.getUserId());
                    entity.setProjectId(event.getProjectId());

                    return entity;
                })
                .flatMap(ticketRepository::save)
                .doOnSuccess(saved -> log.info("Successfully created ticket in DB: {}", saved.getTicketId()))
                .doOnError(error -> log.error("Failed to create ticket: {}", event.getTicketId(), error))
                .then();
    }

    @KafkaListener(
            topics = "ticket-assignments.v1",
            groupId = "ticket-service-assignment-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public Mono<Void> handleTicketAssigned(
            @Payload TicketAssigned event,
            @Header("kafka_receivedTopic") String topic,
            @Header("kafka_receivedPartition") int partition,
            @Header("kafka_offset") long offset) {

        log.info("Processing TicketAssigned event: ticketId={}, assigneeId={}, topic={}, partition={}, offset={}",
                event.getTicketId(), event.getAssigneeId(), topic, partition, offset);

        return ticketRepository.findById(event.getTicketId())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Ticket not found: " + event.getTicketId())))
                .flatMap(ticket -> {
                    ticket.setTicketId(event.getTicketId());
                    ticket.setAssigneeId(event.getAssigneeId());
                    ticket.setUpdatedAt(event.getAssignedAt());
                    return ticketRepository.save(ticket);
                })
                .doOnSuccess(updated -> log.info("Successfully assigned ticket: {} to user: {}",
                        updated.getTicketId(), updated.getAssigneeId()))
                .doOnError(error -> log.error("Failed to assign ticket: {}", event.getTicketId(), error))
                .then();
    }

    @KafkaListener(
            topics = "ticket-updates.v1",
            groupId = "ticket-service-update-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public Mono<Void> handleTicketStatusUpdated(
            @Payload TicketStatusUpdated event,
            @Header("kafka_receivedTopic") String topic,
            @Header("kafka_receivedPartition") int partition,
            @Header("kafka_offset") long offset) {

        log.info("Processing TicketStatusUpdated event: ticketId={}, status={}, topic={}, partition={}, offset={}",
                event.getTicketId(), event.getStatus(), topic, partition, offset);

        return ticketRepository.findById(event.getTicketId())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Ticket not found: " + event.getTicketId())))
                .flatMap(ticket -> {
                    ticket.setStatus(event.getStatus().toUpperCase());
                    ticket.setUpdatedAt(event.getUpdatedAt());
                    return ticketRepository.save(ticket);
                })
                .doOnSuccess(updated -> log.info("Successfully updated ticket status: {} to {}",
                        updated.getTicketId(), updated.getStatus()))
                .doOnError(error -> log.error("Failed to update ticket status: {}", event.getTicketId(), error))
                .then();
    }

    private Integer parsePriority(String priority) {
        if (priority == null || priority.trim().isEmpty()) {
            return null;
        }

        try {
            // Handle common priority formats
            switch (priority.toUpperCase()) {
                case "LOW":
                    return 1;
                case "MEDIUM":
                    return 2;
                case "HIGH":
                    return 3;
                case "URGENT":
                    return 4;
                case "CRITICAL":
                    return 5;
                default:
                    // Try to parse as integer
                    return Integer.parseInt(priority);
            }
        } catch (NumberFormatException e) {
            log.warn("Unable to parse priority: {}, defaulting to null", priority);
            return null;
        }
    }
}


