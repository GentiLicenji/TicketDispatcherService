package com.pleased.ticket.dispatcher.server.util.kafka;

import com.google.protobuf.GeneratedMessageV3;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ProtoBuffSerializer<T extends GeneratedMessageV3> implements Serializer<T> {
    private static final Logger log = LoggerFactory.getLogger(ProtoBuffSerializer.class);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No configuration needed
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }

        try {
            return data.toByteArray();
        } catch (Exception e) {
            log.error("Failed to serialize protobuf message", e);
            throw new RuntimeException("Failed to serialize protobuf message", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}