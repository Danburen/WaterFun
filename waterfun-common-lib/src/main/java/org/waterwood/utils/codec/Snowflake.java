package org.waterwood.utils.codec;

import java.util.concurrent.ThreadLocalRandom;

public final class Snowflake {
    private static final long WORKER_ID_BITS   = 5L;   // 10 bit total of worker & datacenter => 0-1023
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS    = 12L;  // 4096 per millisecond
    private static final long MAX_WORKER_ID    = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK    = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_SHIFT  = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT  = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static final long EPOCH = 1704067200000L;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /* Instance Worker ID use random */
    private static final Snowflake INSTANCE = new Snowflake(
            ThreadLocalRandom.current().nextInt(0, (int) MAX_WORKER_ID + 1),
            ThreadLocalRandom.current().nextInt(0, (int) MAX_DATACENTER_ID + 1));


    private Snowflake(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0)
            throw new IllegalArgumentException("workerId out of range");
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0)
            throw new IllegalArgumentException("datacenterId out of range");
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    @Deprecated
    public static Snowflake of() { return INSTANCE; }

    public static Snowflake of(long workerId, long datacenterId) {
        return new Snowflake(workerId, datacenterId);
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp){
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {  // tolerate tiny rollback only
                try {
                    Thread.sleep(offset);
                    timestamp = System.currentTimeMillis();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException("Clock moved backwards by " + (lastTimestamp - timestamp) + "ms");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for clock recovery", e);
                }
            } else {
                throw new RuntimeException("Clock moved backwards by " + offset + "ms");
            }
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
