package com.pleased.ticket.dispatcher.server.service;

import com.pleased.ticket.dispatcher.server.config.KafkaTopicConfig;
import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.events.TicketStatusUpdated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
public class TicketEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TicketEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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

    private Mono<Void> publishEvent(String topic, UUID key, Object event) {
        return Mono.fromFuture(kafkaTemplate.send(topic, key.toString(), event).completable())
                .doOnSuccess(result -> log.debug("Successfully published event to topic {}: {}", topic, key))
                .doOnError(ex -> log.error("Failed to publish event to topic {}: {}", topic, key, ex))
                .onErrorMap(ex -> new RuntimeException("Failed to publish event", ex))
                .then();
    }

}

