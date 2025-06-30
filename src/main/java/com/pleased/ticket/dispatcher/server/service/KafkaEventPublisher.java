//package com.pleased.ticket.dispatcher.server.service;
//
//
//import com.pleased.ticket.dispatcher.server.model.events.TicketAssigned;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.CompletableFuture;
//
//@Component
//@Slf4j
//public class KafkaEventPublisher {
//
//    private final KafkaTemplate<String, TicketAssigned> kafkaTemplate;
//
//    @Autowired
//    public KafkaEventPublisher(KafkaTemplate<String, TicketAssigned> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    public CompletableFuture<Void> publish(String topic, TicketAssigned event) {
//        return kafkaTemplate.send(topic, event.get(), event)
//                .completable()
//                .thenAccept(result ->
//                        log.info("Event published successfully: {} to topic: {}",
//                                event.getClass().getSimpleName(), topic))
//                .exceptionally(ex -> {
//                    log.error("Failed to publish event: {} to topic: {}",
//                            event.getClass().getSimpleName(), topic, ex);
//                    throw new EventPublishingException("Failed to publish event", ex);
//                });
//    }
//}
