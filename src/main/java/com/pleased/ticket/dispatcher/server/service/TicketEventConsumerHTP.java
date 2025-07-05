//package com.pleased.ticket.dispatcher.server.service;
//
//@Slf4j
//@Service
//public class TicketEventConsumerHTP {
//
//    private final TicketRepository ticketRepository;
//    private final Scheduler scheduler;
//    private final MeterRegistry meterRegistry;
//
//    // Batch processing configuration
//    private static final int BATCH_SIZE = 100;
//    private static final Duration BATCH_TIMEOUT = Duration.ofMillis(50);
//
//    // Metrics
//    private final Counter processedEventsCounter;
//    private final Timer processingTimer;
//    private final Counter errorCounter;
//
//    @Autowired
//    public HighThroughputTicketEventConsumer(
//            TicketRepository ticketRepository,
//            @Qualifier("kafkaScheduler") Scheduler scheduler,
//            MeterRegistry meterRegistry) {
//        this.ticketRepository = ticketRepository;
//        this.scheduler = scheduler;
//        this.meterRegistry = meterRegistry;
//
//        // Initialize metrics
//        this.processedEventsCounter = Counter.builder("ticket.events.processed")
//                .description("Number of ticket events processed")
//                .register(meterRegistry);
//        this.processingTimer = Timer.builder("ticket.events.processing.time")
//                .description("Time taken to process ticket events")
//                .register(meterRegistry);
//        this.errorCounter = Counter.builder("ticket.events.errors")
//                .description("Number of ticket event processing errors")
//                .register(meterRegistry);
//    }
//
//    @KafkaListener(
//            topics = "ticket-create.v1",
//            groupId = "ticket-service-create-consumer-v2",
//            containerFactory = "highThroughputKafkaListenerContainerFactory",
//            concurrency = "#{@kafkaProperties.consumer.concurrency}"
//    )
//    public Mono<Void> handleTicketCreated(
//            @Payload TicketEventsProto.TicketCreated event,
//            @Header("kafka_receivedTopic") String topic,
//            @Header("kafka_receivedPartitionId") int partition,
//            @Header("kafka_offset") long offset,
//            Acknowledgment ack) {
//
//        return processingTimer.recordCallable(() -> {
//            log.debug("Processing TicketCreated: id={}, partition={}, offset={}",
//                    event.getTicketId(), partition, offset);
//
//            return createTicketEntity(event)
//                    .flatMap(this::saveTicketWithRetry)
//                    .doOnSuccess(saved -> {
//                        processedEventsCounter.increment();
//                        ack.acknowledge();
//                        log.debug("Created ticket: {}", saved.getTicketId());
//                    })
//                    .doOnError(error -> {
//                        errorCounter.increment();
//                        log.error("Failed to create ticket: {}, partition: {}, offset: {}",
//                                event.getTicketId(), partition, offset, error);
//                    })
//                    .onErrorResume(this::handleProcessingError)
//                    .then();
//        });
//    }
//
//    @KafkaListener(
//            topics = "ticket-assignments.v1",
//            groupId = "ticket-service-assignment-consumer-v2",
//            containerFactory = "highThroughputKafkaListenerContainerFactory",
//            concurrency = "#{@kafkaProperties.consumer.concurrency}"
//    )
//    public Mono<Void> handleTicketAssigned(
//            @Payload TicketEventsProto.TicketAssigned event,
//            @Header("kafka_receivedTopic") String topic,
//            @Header("kafka_receivedPartitionId") int partition,
//            @Header("kafka_offset") long offset,
//            Acknowledgment ack) {
//
//        return processingTimer.recordCallable(() -> {
//            log.debug("Processing TicketAssigned: id={}, assignee={}, partition={}, offset={}",
//                    event.getTicketId(), event.getAssigneeId(), partition, offset);
//
//            return updateTicketAssignment(event)
//                    .doOnSuccess(updated -> {
//                        processedEventsCounter.increment();
//                        ack.acknowledge();
//                        log.debug("Assigned ticket: {} to {}", updated.getTicketId(), updated.getAssigneeId());
//                    })
//                    .doOnError(error -> {
//                        errorCounter.increment();
//                        log.error("Failed to assign ticket: {}, partition: {}, offset: {}",
//                                event.getTicketId(), partition, offset, error);
//                    })
//                    .onErrorResume(this::handleProcessingError)
//                    .then();
//        });
//    }
//
//    @KafkaListener(
//            topics = "ticket-updates.v1",
//            groupId = "ticket-service-update-consumer-v2",
//            containerFactory = "highThroughputKafkaListenerContainerFactory",
//            concurrency = "#{@kafkaProperties.consumer.concurrency}"
//    )
//    public Mono<Void> handleTicketStatusUpdated(
//            @Payload TicketEventsProto.TicketStatusUpdated event,
//            @Header("kafka_receivedTopic") String topic,
//            @Header("kafka_receivedPartitionId") int partition,
//            @Header("kafka_offset") long offset,
//            Acknowledgment ack) {
//
//        return processingTimer.recordCallable(() -> {
//            log.debug("Processing TicketStatusUpdated: id={}, status={}, partition={}, offset={}",
//                    event.getTicketId(), event.getStatus(), partition, offset);
//
//            return updateTicketStatus(event)
//                    .doOnSuccess(updated -> {
//                        processedEventsCounter.increment();
//                        ack.acknowledge();
//                        log.debug("Updated ticket status: {} to {}", updated.getTicketId(), updated.getStatus());
//                    })
//                    .doOnError(error -> {
//                        errorCounter.increment();
//                        log.error("Failed to update ticket status: {}, partition: {}, offset: {}",
//                                event.getTicketId(), partition, offset, error);
//                    })
//                    .onErrorResume(this::handleProcessingError)
//                    .then();
//        });
//    }
//
//    // Batch processing for high throughput scenarios
//    @KafkaListener(
//            topics = "ticket-batch.v1",
//            groupId = "ticket-service-batch-consumer",
//            containerFactory = "batchKafkaListenerContainerFactory"
//    )
//    public Mono<Void> handleTicketBatch(
//            @Payload List<TicketEventsProto.TicketEventBatch> batches,
//            @Header("kafka_receivedTopic") String topic,
//            @Header("kafka_receivedPartitionId") int partition,
//            @Header("kafka_offset") List<Long> offsets,
//            Acknowledgment ack) {
//
//        return processingTimer.recordCallable(() -> {
//            log.debug("Processing batch of {} events from partition {}", batches.size(), partition);
//
//            return Flux.fromIterable(batches)
//                    .flatMap(this::processBatch, 4) // Process 4 batches concurrently
//                    .collectList()
//                    .doOnSuccess(results -> {
//                        processedEventsCounter.increment(results.size());
//                        ack.acknowledge();
//                        log.debug("Processed batch of {} events", results.size());
//                    })
//                    .doOnError(error -> {
//                        errorCounter.increment();
//                        log.error("Failed to process batch from partition {}", partition, error);
//                    })
//                    .onErrorResume(this::handleProcessingError)
//                    .then();
//        });
//    }
//
//    private Mono<TicketEntity> createTicketEntity(TicketEventsProto.TicketCreated event) {
//        return Mono.fromCallable(() -> {
//            TicketEntity entity = new TicketEntity();
//            entity.setTicketId(event.getTicketId());
//            entity.setSubject(event.getSubject());
//            entity.setDescription(event.getDescription());
//            entity.setStatus(TicketStatusEnum.OPEN.toString());
//            entity.setCreatedAt(Instant.ofEpochMilli(event.getCreatedAt()).atOffset(ZoneOffset.UTC));
//            entity.setUserId(event.getUserId());
//            entity.setProjectId(event.getProjectId());
//            return entity;
//        }).subscribeOn(scheduler);
//    }
//
//    private Mono<TicketEntity> updateTicketAssignment(TicketEventsProto.TicketAssigned event) {
//        return ticketRepository.findById(event.getTicketId())
//                .switchIfEmpty(Mono.error(new EntityNotFoundException("Ticket not found: " + event.getTicketId())))
//                .map(ticket -> {
//                    ticket.setAssigneeId(event.getAssigneeId());
//                    ticket.setUpdatedAt(Instant.ofEpochMilli(event.getAssignedAt()).atOffset(ZoneOffset.UTC));
//                    ticket.setNew(false);
//                    return ticket;
//                })
//                .flatMap(this::saveTicketWithRetry)
//                .subscribeOn(scheduler);
//    }
//
//    private Mono<TicketEntity> updateTicketStatus(TicketEventsProto.TicketStatusUpdated event) {
//        return ticketRepository.findById(event.getTicketId())
//                .switchIfEmpty(Mono.error(new EntityNotFoundException("Ticket not found: " + event.getTicketId())))
//                .map(ticket -> {
//                    ticket.setStatus(event.getStatus().toUpperCase());
//                    ticket.setUpdatedAt(Instant.ofEpochMilli(event.getUpdatedAt()).atOffset(ZoneOffset.UTC));
//                    ticket.setNew(false);
//                    return ticket;
//                })
//                .flatMap(this::saveTicketWithRetry)
//                .subscribeOn(scheduler);
//    }
//
//    private Mono<List<TicketEntity>> processBatch(TicketEventsProto.TicketEventBatch batch) {
//        List<Mono<TicketEntity>> operations = new ArrayList<>();
//
//        // Process created events
//        for (TicketEventsProto.TicketCreated event : batch.getCreatedEventsList()) {
//            operations.add(createTicketEntity(event).flatMap(this::saveTicketWithRetry));
//        }
//
//        // Process assignment events
//        for (TicketEventsProto.TicketAssigned event : batch.getAssignedEventsList()) {
//            operations.add(updateTicketAssignment(event));
//        }
//
//        // Process status update events
//        for (TicketEventsProto.TicketStatusUpdated event : batch.getStatusUpdatedEventsList()) {
//            operations.add(updateTicketStatus(event));
//        }
//
//        return Flux.fromIterable(operations)
//                .flatMap(mono -> mono, 8) // Process 8 operations concurrently
//                .collectList();
//    }
//
//    private Mono<TicketEntity> saveTicketWithRetry(TicketEntity ticket) {
//        return ticketRepository.save(ticket)
//                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
//                        .filter(throwable -> throwable instanceof DataAccessException)
//                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
//                                new RuntimeException("Failed to save ticket after retries: " + ticket.getTicketId(),
//                                        retrySignal.failure())));
//    }
//
//    private Mono<Void> handleProcessingError(Throwable error) {
//        // Log error but don't fail the consumer
//        log.error("Processing error handled: {}", error.getMessage());
//        return Mono.empty();
//    }
//}
