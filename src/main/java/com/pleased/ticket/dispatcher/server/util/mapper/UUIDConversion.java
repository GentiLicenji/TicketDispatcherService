package com.pleased.ticket.dispatcher.server.util.mapper;

import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDConversion extends Conversion<UUID> {

    @Override
    public Class<UUID> getConvertedType() {
        return UUID.class;
    }

    @Override
    public String getLogicalTypeName() {
        return "uuid";
    }

    @Override
    public UUID fromBytes(ByteBuffer value, Schema schema, LogicalType type) {
        return UUIDConverter.bytesToUUID(value);
    }

    @Override
    public ByteBuffer toBytes(UUID value, Schema schema, LogicalType type) {
        return UUIDConverter.uuidToBytes(value);
    }
}