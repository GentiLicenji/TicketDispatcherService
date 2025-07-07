package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.config.KafkaTopicConfig;
import com.pleased.ticket.dispatcher.server.exception.EventPublishingException;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import com.pleased.ticket.dispatcher.server.util.mapper.UUIDConverter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderRecord;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    public Mono<Void> publishTicketCreated(TicketCreated event, UUID correlationId) {
        return publishEvent(KafkaTopicConfig.TICKET_CREATE_TOPIC, event.getTicketId(), event,correlationId);
    }

    public Mono<Void> publishTicketAssigned(TicketAssigned event,UUID correlationId) {
        return publishEvent(KafkaTopicConfig.TICKET_ASSIGNMENTS_TOPIC, event.getTicketId(), event,correlationId);
    }

    public Mono<Void> publishTicketStatusUpdated(TicketStatusUpdated event,UUID correlationId) {
        return publishEvent(KafkaTopicConfig.TICKET_UPDATES_TOPIC, event.getTicketId(), event,correlationId);
    }

    private Mono<Void> publishEvent(String topic, ByteBuffer key, SpecificRecordBase event, UUID correlationId) {
        // Validate inputs
        if (event == null) {
            return Mono.error(new IllegalArgumentException("Event cannot be null"));
        }
        if (key == null) {
            return Mono.error(new IllegalArgumentException("Key cannot be null"));
        }

        // Create SenderRecord with headers
        SenderRecord<ByteBuffer, Object, ByteBuffer> record = SenderRecord.create(
                topic,
                null, // Let Kafka decide partition based on key
                null, // Let Kafka set timestamp
                key,
                event,
                UUIDConverter.uuidToBytes(correlationId)  // correlation metadata
        );
        // Set headers
        record.headers()
                .add("eventType", KafkaTopicConfig.EVENT_TYPE_MAP.get(topic).getBytes());//TODO maybe can be used in the future.

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
}

