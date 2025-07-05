package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.config.KafkaTopicConfig;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.protocol.Message;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketEventProducer {

    private final KafkaTemplate<String, Message> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public Mono<Void> publishTicketCreated(TicketCreated event) {
        return Mono.create(sink -> {
            String ticketId = ""; // UUIDHelper.protobufUUIDToString(event.getTicketId());

            ProducerRecord<String, Message> record = new ProducerRecord<>(
                    KafkaTopicConfig.TICKET_CREATE_TOPIC,
                    ticketId,
                    event
            );
            addHeaders(record, event.getEventId().toString(), "TICKET_CREATED");

            kafkaTemplate.send(record)
                    .addCallback(result -> {
                        log.debug("Published TicketCreated event: {} to partition: {}",
                                ticketId, result.getRecordMetadata().partition());
                        meterRegistry.counter("ticket.events.publish.success", "type", "created").increment();
                        sink.success();
                    }, ex -> {
                        log.error("Failed to publish TicketCreated event: {}", ticketId, ex);
                        meterRegistry.counter("ticket.events.publish.failed", "type", "created").increment();
                        sink.success(); // Complete even on failure to prevent blocking
                    });
        });
    }

    public Mono<Void> publishTicketAssigned(TicketAssigned event) {
        return Mono.create(sink -> {
            String ticketId = ""; // UUIDHelper.protobufUUIDToString(event.getTicketId());

            ProducerRecord<String, Message> record = new ProducerRecord<>(
                    KafkaTopicConfig.TICKET_ASSIGNMENTS_TOPIC,
                    ticketId,
                    event
            );
            addHeaders(record, event.getEventId().toString(), "TICKET_ASSIGNED");

            kafkaTemplate.send(record)
                    .addCallback(result -> {
                        log.debug("Published TicketAssigned event: {}", ticketId);
                        meterRegistry.counter("ticket.events.publish.success", "type", "assigned").increment();
                        sink.success();
                    }, ex -> {
                        log.error("Failed to publish TicketAssigned event: {}", ticketId, ex);
                        meterRegistry.counter("ticket.events.publish.failed", "type", "assigned").increment();
                        sink.success();
                    });
        });
    }

    public Mono<Void> publishTicketStatusUpdated(TicketStatusUpdated event) {
        return Mono.create(sink -> {
            String ticketId = ""; // UUIDHelper.protobufUUIDToString(event.getTicketId());

            ProducerRecord<String, Message> record = new ProducerRecord<>(
                    KafkaTopicConfig.TICKET_UPDATES_TOPIC,
                    ticketId,
                    event
            );
            addHeaders(record, event.getEventId().toString(), "TICKET_STATUS_UPDATED");

            kafkaTemplate.send(record)
                    .addCallback(result -> {
                        log.debug("Published TicketStatusUpdated event: {}", ticketId);
                        meterRegistry.counter("ticket.events.publish.success", "type", "status_updated").increment();
                        sink.success();
                    }, ex -> {
                        log.error("Failed to publish TicketStatusUpdated event: {}", ticketId, ex);
                        meterRegistry.counter("ticket.events.publish.failed", "type", "status_updated").increment();
                        sink.success();
                    });
        });
    }

    private void addHeaders(ProducerRecord<String, Message> record, String idempotencyKey, String eventType) {
        record.headers().add("eventId", idempotencyKey.getBytes());
        record.headers().add("correlationId", UUID.randomUUID().toString().getBytes());
        record.headers().add("eventType", eventType.getBytes());
        record.headers().add("publishedAt", String.valueOf(System.currentTimeMillis()).getBytes());
    }
}
