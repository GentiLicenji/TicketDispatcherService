package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.exception.EntityNotFoundException;
import com.pleased.ticket.dispatcher.server.model.dto.TicketEntity;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.model.rest.TicketResponse;
import com.pleased.ticket.dispatcher.server.repository.TicketRepository;
import com.pleased.ticket.dispatcher.server.util.mapper.UUIDConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
public class TicketEventConsumer {

    private final TicketRepository ticketRepository;

    private final ReactiveKafkaConsumerTemplate<ByteBuffer, TicketCreated> reactiveTicketCreatedConsumer;
    private final ReactiveKafkaConsumerTemplate<ByteBuffer, TicketAssigned> reactiveTicketAssignmentConsumer;
    private final ReactiveKafkaConsumerTemplate<ByteBuffer, TicketStatusUpdated> reactiveTicketUpdateConsumer;

    @Autowired
    public TicketEventConsumer(TicketRepository ticketRepository, ReactiveKafkaConsumerTemplate<ByteBuffer, TicketCreated> reactiveTicketCreatedConsumer, ReactiveKafkaConsumerTemplate<ByteBuffer, TicketAssigned> reactiveTicketAssignmentConsumer, ReactiveKafkaConsumerTemplate<ByteBuffer, TicketStatusUpdated> reactiveTicketUpdateConsumer) {
        this.ticketRepository = ticketRepository;
        this.reactiveTicketCreatedConsumer = reactiveTicketCreatedConsumer;
        this.reactiveTicketAssignmentConsumer = reactiveTicketAssignmentConsumer;
        this.reactiveTicketUpdateConsumer = reactiveTicketUpdateConsumer;
    }

    @PostConstruct
    public void startConsuming() {
        //Start create consumer
        reactiveTicketCreatedConsumer.receiveAutoAck()
                .doOnNext(record -> log.info("Processing ticket creation: {}", record.value()))
                .flatMap(this::handleTicketCreated)
                .doOnError(error -> log.error("Error processing ticket creation", error))
                .retry(3)
                .subscribe();

        // Start assignment consumer
        reactiveTicketAssignmentConsumer.receiveAutoAck()
                .doOnNext(record -> log.info("Processing ticket assignment: {}", record.value()))
                .flatMap(this::handleTicketAssigned)
                .doOnError(error -> log.error("Error processing assignment", error))
                .retry(3)
                .subscribe();

        // Start update consumer
        reactiveTicketUpdateConsumer.receiveAutoAck()
                .doOnNext(record -> log.info("Processing ticket update: {}", record.value()))
                .flatMap(this::handleTicketStatusUpdated)
                .doOnError(error -> log.error("Error processing ticket update", error))
                .retry(3)
                .subscribe();
    }

    public Mono<Void> handleTicketCreated(ConsumerRecord<ByteBuffer, TicketCreated> record) {

        TicketCreated event = record.value();

        // Create entity directly in the reactive chain
        TicketEntity entity = new TicketEntity();
        entity.setTicketId(UUIDConverter.bytesToUUID(event.getTicketId()));
        entity.setSubject(event.getSubject());
        entity.setDescription(event.getDescription());
        entity.setStatus(TicketResponse.StatusEnum.OPEN.toString());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUserId(UUIDConverter.bytesToUUID(event.getUserId()));
        entity.setProjectId(UUIDConverter.bytesToUUID(event.getProjectId()));


        return ticketRepository.save(entity)
                .doOnSubscribe(subscription -> log.info("Someone subscribed to the save operation!"))
                .doOnNext(saved -> log.info("Successfully created ticket in DB: {}", saved.getTicketId()))
                .doOnError(error -> log.error("Failed to create ticket: {}", event.getTicketId(), error))
                .then(); // Convert Mono<TicketEntity> to Mono<Void>
    }

    public Mono<Void> handleTicketAssigned(ConsumerRecord<ByteBuffer, TicketAssigned> record) {

        TicketAssigned event = record.value();

        return ticketRepository.findById(UUIDConverter.bytesToUUID(event.getTicketId()))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Ticket not found: " + UUIDConverter.bytesToUUID(event.getTicketId()))))
                .flatMap(ticket -> {
                    ticket.setAssigneeId(UUIDConverter.bytesToUUID(event.getAssigneeId()));
                    ticket.setUpdatedAt(event.getAssignedAt().atOffset(ZoneOffset.UTC));

                    // Mark as not new since we're updating
                    ticket.setNew(false);
                    return ticketRepository.save(ticket);
                })
                .doOnSuccess(updated -> log.info("Successfully assigned ticket: {} to user: {}",
                        updated.getTicketId(), updated.getAssigneeId()))
                .doOnError(error -> log.error("Failed to assign ticket: {}", event.getTicketId(), error))
                .then();
    }

    public Mono<Void> handleTicketStatusUpdated(ConsumerRecord<ByteBuffer, TicketStatusUpdated> record) {

        TicketStatusUpdated event = record.value();

        return ticketRepository.findById(UUIDConverter.bytesToUUID(event.getTicketId()))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Ticket not found: " +  UUIDConverter.bytesToUUID(event.getTicketId()))))
                .flatMap(ticket -> {
                    ticket.setStatus(event.getStatus().toUpperCase());
                    ticket.setUpdatedAt(event.getUpdatedAt().atOffset(ZoneOffset.UTC));

                    // Mark as not new since we're updating
                    ticket.setNew(false);
                    return ticketRepository.save(ticket);
                })
                .doOnSuccess(updated -> log.info("Successfully updated ticket status: {} to {}",
                        updated.getTicketId(), updated.getStatus()))
                .doOnError(error -> log.error("Failed to update ticket status: {}", event.getTicketId(), error))
                .then();
    }
    //TODO: After implementing ticket detail update API we can map priority and other fields!
}


