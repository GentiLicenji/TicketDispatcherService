//package com.pleased.ticket.dispatcher.server.service;
//
//import com.google.protobuf.util.JsonFormat;
//import com.pleased.ticket.dispatcher.server.model.dto.TicketEntity;
//import com.pleased.ticket.dispatcher.server.repository.TicketRepository;
//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.common.header.Header;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//import static org.mapstruct.ap.shaded.freemarker.template.utility.StringUtil.capitalize;
//import static org.springframework.messaging.rsocket.PayloadUtils.createPayload;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class TicketEventBatchConsumer {
//
//    private final TicketRepository repository;
//    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
//    private final MeterRegistry meterRegistry;
//    private final Timer processingTimer;
//    private final Counter processedEventsCounter;
//    private final Counter errorCounter;
//
//    @Autowired
//    public TicketEventBatchConsumer(
//            TicketRepository repository,
//            io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry,
//            MeterRegistry meterRegistry
//    ) {
//        this.repository = repository;
//        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("ticket-db");
//        this.meterRegistry = meterRegistry;
//        this.processingTimer = Timer.builder("ticket.events.processing.duration").register(meterRegistry);
//        this.processedEventsCounter = Counter.builder("ticket.events.processed.total").register(meterRegistry);
//        this.errorCounter = Counter.builder("ticket.events.errors.total").register(meterRegistry);
//    }
//
//    @KafkaListener(
//            topics = "ticket-create.v1",
//            groupId = "ticket-service-create-consumer-v2",
//            containerFactory = "ticketCreatedBatchListenerContainerFactory"
//    )
//    public Mono<Void> handleTicketCreatedBatch(List<ConsumerRecord<String, TicketEventsProto.TicketCreated>> records, Acknowledgment ack) {
//        return processBatch(records, ack, "TICKET_CREATED");
//    }
//
//    @KafkaListener(
//            topics = "ticket-assignments.v1",
//            groupId = "ticket-service-assignment-consumer-v2",
//            containerFactory = "ticketAssignedBatchListenerContainerFactory"
//    )
//    public Mono<Void> handleTicketAssignedBatch(List<ConsumerRecord<String, TicketEventsProto.TicketAssigned>> records, Acknowledgment ack) {
//        return processBatch(records, ack, "TICKET_ASSIGNED");
//    }
//
//    @KafkaListener(
//            topics = "ticket-updates.v1",
//            groupId = "ticket-service-update-consumer-v2",
//            containerFactory = "ticketStatusUpdatedBatchListenerContainerFactory"
//    )
//    public Mono<Void> handleTicketStatusUpdatedBatch(List<ConsumerRecord<String, TicketEventsProto.TicketStatusUpdated>> records, Acknowledgment ack) {
//        return processBatch(records, ack, "TICKET_STATUS_UPDATED");
//    }
//
//    private <T> Mono<Void> processBatch(List<ConsumerRecord<String, T>> records, Acknowledgment ack, String eventType) {
//        return Mono.create(sink -> {
//            long start = System.nanoTime();
//            try {
//                log.info("Processing batch of {} {} events", records.size(), eventType);
//
//                List<TicketEntity> entities = records.stream()
//                        .map(record -> mapToEntity(record, eventType))
//                        .collect(Collectors.toList());
//
//                Mono<Integer> saveMono = null;
//
//                if ("TICKET_CREATED".equals(eventType)) {
//                    saveMono = repository.saveAll(entities);
//                } else if ("TICKET_ASSIGNED".equals(eventType)) {
//                    saveMono = repository.saveAllTicketAssigned(entities);
//                } else if ("TICKET_STATUS_UPDATED".equals(eventType)) {
//                    saveMono = repository.saveAllTicketStatusUpdated(entities);
//                }
//
//                final Mono<Integer> finalSaveMono = saveMono;
//
//                io.vavr.control.Try<Void> result = circuitBreaker.executeTry(() -> {
//                    finalSaveMono
//                            .doOnSuccess(count -> {
//                                processedEventsCounter.increment(count);
//                                ack.acknowledge();
//                                log.info("Successfully processed batch of {} events", count);
//                                sink.success();
//                            })
//                            .doOnError(ex -> {
//                                errorCounter.increment();
//                                log.error("Failed to process batch of {} events", eventType, ex);
//                                sink.success(); // fail safe
//                            })
//                            .subscribe();
//                    return null;
//                });
//
//                long end = System.nanoTime();
//                processingTimer.record(end - start, TimeUnit.NANOSECONDS);
//
//                if (result.isFailure()) {
//                    log.error("Circuit breaker blocked {} processing", eventType, result.getCause());
//                    sink.success(); // complete anyway to avoid stuck Kafka ack
//                }
//
//            } catch (Throwable ex) {
//                errorCounter.increment();
//                log.error("Unexpected error during batch processing of {}", eventType, ex);
//                sink.success(); // complete even on error
//            }
//        });
//    }
//
//    private TicketEntity mapToEntity(ConsumerRecord<String, ?> record, String type) {
//        Object event = record.value();
//        TicketEntity. builder = TicketEntity.builder()
//                .eventId(extractUUIDFromHeader(record, "eventId"))
//                .correlationId(extractUUIDFromHeader(record, "correlationId"))
//                .ticketId(UUID.fromString(extractField(event, "ticketId")))
//                .eventType(type)
//                .eventTimestamp(Instant.ofEpochMilli(Long.parseLong(extractField(event, getTimestampField(type)))))
//                .partition(record.partition())
//                .offset(record.offset())
//                .payload(createPayload(event))
//                .processedAt(Instant.now());
//
//        if ("TICKET_ASSIGNED".equals(type)) {
//            builder.assigneeId(UUID.fromString(extractField(event, "assigneeId")));
//        } else if ("TICKET_CREATED".equals(type)) {
//            builder.userId(UUID.fromString(extractField(event, "userId")));
//            builder.projectId(UUID.fromString(extractField(event, "projectId")));
//        }
//
//        return builder.build();
//    }
//
//    private String extractField(Object event, String field) {
//        try {
//            return event.getClass().getMethod("get" + capitalize(field)).invoke(event).toString();
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to extract field: " + field, e);
//        }
//    }
//
//    private String getTimestampField(String type) {
//        switch (type) {
//            case "TICKET_CREATED":
//                return "createdAt";
//            case "TICKET_ASSIGNED":
//                return "assignedAt";
//            case "TICKET_STATUS_UPDATED":
//                return "updatedAt";
//            default:
//                return "timestamp";
//        }
