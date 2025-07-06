package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.config.KafkaTopicConfig;
import com.pleased.ticket.dispatcher.server.exception.EventPublishingException;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderRecord;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class TicketEventProducer {

    private final ReactiveKafkaProducerTemplate<ByteBuffer, Object> reactiveKafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Autowired
    public TicketEventProducer(ReactiveKafkaProducerTemplate<ByteBuffer, Object> reactiveKafkaTemplate, MeterRegistry meterRegistry) {
        this.reactiveKafkaTemplate = reactiveKafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    public Mono<Void> publishTicketCreated(TicketCreated event) {
        return publishEvent(KafkaTopicConfig.TICKET_CREATE_TOPIC, event.getTicketId(), event);
    }

    public Mono<Void> publishTicketAssigned(TicketAssigned event) {
        return publishEvent(KafkaTopicConfig.TICKET_ASSIGNMENTS_TOPIC, event.getTicketId(), event);
    }

    public Mono<Void> publishTicketStatusUpdated(TicketStatusUpdated event) {
        return publishEvent(KafkaTopicConfig.TICKET_UPDATES_TOPIC, event.getTicketId(), event);
    }

//    private Mono<Void> publishEvent(String topic, ByteBuffer key, Object event) {
//        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);
//        addEventHeaders(record.headers(), event);
//
//        return Mono.fromFuture(kafkaTemplate.send(record).completable())
//                .doOnSuccess(result -> log.debug("Successfully published event to topic {}: {}", topic, key))
//                .doOnError(ex -> log.error("Failed to publish event to topic {}: {}", topic, key, ex))
//                .onErrorMap(ex -> new RuntimeException("Failed to publish event", ex))
//                .then();
//    }

    private Mono<Void> publishEvent(String topic, ByteBuffer key, Object event) {
        // Validate inputs
        if (event == null) {
            return Mono.error(new IllegalArgumentException("Event cannot be null"));
        }
        if (key == null) {
            return Mono.error(new IllegalArgumentException("Key cannot be null"));
        }

        // Create SenderRecord with headers
        SenderRecord<ByteBuffer, Object, Void> record = SenderRecord.create(
                topic,
                null, // Let Kafka decide partition based on key
                null, // Let Kafka set timestamp
                key,
                event,
                null  // correlation metadata
        );

        // Add headers to the record
//        addEventHeaders(record.headers(), event);

        Timer.Sample sample = Timer.start(meterRegistry);

        return reactiveKafkaTemplate.send(record)
                .doOnSuccess(result -> {
                    sample.stop(Timer.builder("kafka.producer.send.duration")
                            .tag("topic", topic)
                            .tag("status", "success")
                            .register(meterRegistry));
                    meterRegistry.counter("kafka.producer.send.total", "topic", topic, "status", "success").increment();

                    RecordMetadata metadata = result.recordMetadata();
                    log.debug("Successfully published event to topic {}: {} - partition: {}, offset: {}",
                            topic, key, metadata.partition(), metadata.offset());
                })
                .doOnError(ex -> {
                    sample.stop(Timer.builder("kafka.producer.send.duration")
                            .tag("topic", topic)
                            .tag("status", "error")
                            .register(meterRegistry));
                    meterRegistry.counter("kafka.producer.send.total", "topic", topic, "status", "error").increment();
                    log.error("Failed to publish event to topic {}: {} - Error: {}", topic, key, ex.getMessage(), ex);
                })
                .onErrorMap(ex -> new EventPublishingException("Failed to publish event to topic: " + topic, ex))
                .then();// Converts Mono<SenderResult<T>> to Mono<Void>
    }

//    private SenderRecord<ByteBuffer, TicketCreated, String> prepareRecord(
//            String topic, ByteBuffer ticketId, TicketCreated event) {
//
//        // Validate the event
////        validateTicketCreated(event);
//
//        // Create correlation ID for tracking
//        String correlationId = UUID.randomUUID().toString();
//
//        // Create headers
//        Headers headers = createHeaders(correlationId, "TicketCreated");
//
//        return SenderRecord.create(
//                topic,
//                null, // Let Kafka decide partition based on key
//                null, // Let Kafka set timestamp
//                ticketId,
//                event,
//                correlationId // correlation metadata
//        ).headers(headers);
//    }

//    private Headers createHeaders(String correlationId, String eventType) {
//        Headers headers = new RecordHeaders();
//        headers.add("event-type", eventType.getBytes(StandardCharsets.UTF_8));
//        headers.add("event-version", "1.0".getBytes(StandardCharsets.UTF_8));
//        headers.add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
//        headers.add("producer-service", "ticket-service".getBytes(StandardCharsets.UTF_8));
//        headers.add("schema-version", String.valueOf(event.getSchema().hashCode()).getBytes(StandardCharsets.UTF_8));
//        return headers;
//    }

    private void validateTicketCreated(TicketCreated event) {
        if (event.getTicketId() == null) {
            throw new IllegalArgumentException("TicketCreated event must have ticketId");
        }
        if (event.getCreatedAt() == null) {
            throw new IllegalArgumentException("TicketCreated event must have createdAt timestamp");
        }
        // Add other validation rules
    }

    private String extractTicketIdForLogging(TicketCreated event) {
        if (event.getTicketId() != null) {
            return new String(event.getTicketId().array(), StandardCharsets.UTF_8);
        }
        return "unknown";
    }
//    private void addEventHeaders(Headers headers, TicketEvent event) {
//        if (event.getEventId() != null) {
//            headers.add(new RecordHeader("eventId", event.getEventId().toString().getBytes(StandardCharsets.UTF_8)));
//        }
//        if (event.getCorrelationId() != null) {
//            headers.add(new RecordHeader("correlationId", event.getCorrelationId().toString().getBytes(StandardCharsets.UTF_8)));
//        }
//        headers.add(new RecordHeader("eventVersion", Integer.toString(event.getEventVersion()).getBytes(StandardCharsets.UTF_8)));
//    }
}

