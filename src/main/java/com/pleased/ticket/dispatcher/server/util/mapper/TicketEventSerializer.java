package com.pleased.ticket.dispatcher.server.util.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pleased.ticket.dispatcher.server.model.events.TicketEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class TicketEventSerializer extends JsonSerializer<Object> {

    @Override
    public byte[] serialize(String topic, Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            if (data instanceof TicketEvent) {
                ObjectNode node = mapper.valueToTree(data);

                // Add eventType based on the class name
                node.put("eventType", data.getClass().getSimpleName());
                return mapper.writeValueAsBytes(node);
            }

            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing object", e);
        }
    }
}