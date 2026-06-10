package org.waterwood.waterfunservicecore.dto;

import lombok.Data;

@Data
public class JvmInfoVO {
    private String source;
    private Jvm jvm;
    private Heap heap;
    private NonHeap nonHeap;
    private Thread thread;
    private Os os;

    @Data
    public static class Jvm {
        private String name;
        private String version;
        private long uptime;
    }

    @Data
    public static class Heap {
        private long used;
        private long committed;
        private long max;
    }

    @Data
    public static class NonHeap {
        private long used;
    }

    @Data
    public static class Thread {
        private int count;
    }

    @Data
    public static class Os {
        private int availableProcessors;
        private long totalMemory;
        private long freeMemory;
    }
}
