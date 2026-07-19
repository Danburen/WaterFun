package org.waterwood.waterfunadminservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunadminservice.api.response.monitor.SystemInfoVO;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class MonitorService {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hal;
    private final CentralProcessor cpu;
    private final OperatingSystem os;

    private long[][] prevCpuTicks;
    private Instant prevCpuSampleTime;

    public MonitorService() {
        this.systemInfo = new SystemInfo();
        this.hal = systemInfo.getHardware();
        this.cpu = hal.getProcessor();
        this.os = systemInfo.getOperatingSystem();
    }

    @PostConstruct
    void init() {
        prevCpuTicks = cpu.getProcessorCpuLoadTicks();
        prevCpuSampleTime = Instant.now();
    }

    public SystemInfoVO collect() {
        SystemInfoVO vo = new SystemInfoVO();
        vo.setCpu(collectCpu());
        vo.setMemory(collectMemory());
        vo.setDisks(collectDisks());
        vo.setNetwork(collectNetwork());
        vo.setSystem(collectSystem());
        return vo;
    }

    private SystemInfoVO.CpuInfoVO collectCpu() {
        var identifier = cpu.getProcessorIdentifier();

        long[][] currentTicks = cpu.getProcessorCpuLoadTicks();
        double usage = 0;
        if (Duration.between(prevCpuSampleTime, Instant.now()).toMillis() > 500) {
            double[] perCpu = cpu.getProcessorCpuLoadBetweenTicks(prevCpuTicks, currentTicks);
            double total = 0;
            for (double d : perCpu) total += Math.max(d, 0);
            usage = perCpu.length > 0 ? total / perCpu.length : 0;
            prevCpuTicks = currentTicks;
            prevCpuSampleTime = Instant.now();
        }

        double[] loadAvg = cpu.getSystemLoadAverage(3);

        var vo = new SystemInfoVO.CpuInfoVO();
        vo.setName(identifier.getName().trim());
        vo.setPhysicalCores(cpu.getPhysicalProcessorCount());
        vo.setLogicalCores(cpu.getLogicalProcessorCount());
        vo.setUsage(Math.round(usage * 10000.0) / 10000.0);
        vo.setLoad1m(loadAvg.length > 0 ? round1(loadAvg[0]) : -1);
        vo.setLoad5m(loadAvg.length > 1 ? round1(loadAvg[1]) : -1);
        vo.setLoad15m(loadAvg.length > 2 ? round1(loadAvg[2]) : -1);
        return vo;
    }

    private SystemInfoVO.MemoryInfoVO collectMemory() {
        GlobalMemory memory = hal.getMemory();

        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        var vo = new SystemInfoVO.MemoryInfoVO();
        vo.setTotal(total);
        vo.setUsed(used);
        vo.setAvailable(available);
        vo.setUsage(total > 0 ? round4((double) used / total) : 0);
        return vo;
    }

    private List<SystemInfoVO.DiskInfoVO> collectDisks() {
        List<SystemInfoVO.DiskInfoVO> list = new ArrayList<>();
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                long total = store.getTotalSpace();
                long free = store.getUsableSpace();
                long used = total - free;

                var vo = new SystemInfoVO.DiskInfoVO();
                vo.setMount(store.toString());
                vo.setType(store.type());
                vo.setTotal(total);
                vo.setUsed(used);
                vo.setFree(free);
                vo.setUsage(total > 0 ? round4((double) used / total) : 0);
                list.add(vo);
            }
        } catch (Exception e) {
            log.warn("Failed to collect disk info", e);
        }
        list.sort(Comparator.comparing(SystemInfoVO.DiskInfoVO::getMount));
        return list;
    }

    private SystemInfoVO.NetworkInfoVO collectNetwork() {
        long sent = 0;
        long received = 0;
        List<NetworkIF> nifs = hal.getNetworkIFs();
        for (NetworkIF nif : nifs) {
            nif.updateAttributes();
            sent += nif.getBytesSent();
            received += nif.getBytesRecv();
        }
        var vo = new SystemInfoVO.NetworkInfoVO();
        vo.setSent(sent);
        vo.setReceived(received);
        vo.setTimestamp(System.currentTimeMillis());
        return vo;
    }

    private SystemInfoVO.SystemDetailVO collectSystem() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }

        var vo = new SystemInfoVO.SystemDetailVO();
        vo.setOs(os.toString());
        vo.setHostname(hostname);
        vo.setArch(System.getProperty("os.arch"));
        vo.setUptime(os.getSystemUptime());
        return vo;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
