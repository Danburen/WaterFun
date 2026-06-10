package org.waterwood.waterfunadminservice.api.response.monitor;

import lombok.Data;

import java.util.List;

@Data
public class SystemInfoVO {
    private CpuInfoVO cpu;
    private MemoryInfoVO memory;
    private List<DiskInfoVO> disks;
    private NetworkInfoVO network;
    private SystemDetailVO system;
    private MergedJvmInfo jvms;

    @Data
    public static class CpuInfoVO {
        private String name;
        private int physicalCores;
        private int logicalCores;
        private double usage;
        private double load1m;
        private double load5m;
        private double load15m;
    }

    @Data
    public static class MemoryInfoVO {
        private long total;
        private long used;
        private long available;
        private double usage;
    }

    @Data
    public static class DiskInfoVO {
        private String mount;
        private String type;
        private long total;
        private long used;
        private long free;
        private double usage;
    }

    @Data
    public static class NetworkInfoVO {
        private long sent;
        private long received;
        private long timestamp;
    }

    @Data
    public static class SystemDetailVO {
        private String os;
        private String hostname;
        private String arch;
        private long uptime;
    }
}
