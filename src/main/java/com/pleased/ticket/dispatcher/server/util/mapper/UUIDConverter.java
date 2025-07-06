package com.pleased.ticket.dispatcher.server.util.mapper;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDConverter {

    public static ByteBuffer uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        buffer.flip();
        return buffer;
    }

    public static UUID bytesToUUID(ByteBuffer buffer) {
        buffer.rewind();
        long mostSig = buffer.getLong();
        long leastSig = buffer.getLong();
        return new UUID(mostSig, leastSig);
    }
}