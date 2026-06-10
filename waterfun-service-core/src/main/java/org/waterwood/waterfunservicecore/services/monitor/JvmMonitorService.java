package org.waterwood.waterfunservicecore.services.monitor;

import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.dto.JvmInfoVO;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

@Service
public class JvmMonitorService {

    public JvmInfoVO collect(String source) {
        Runtime rt = Runtime.getRuntime();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();

        var jvm = new JvmInfoVO.Jvm();
        jvm.setName(runtime.getVmName());
        jvm.setVersion(System.getProperty("java.version"));
        jvm.setUptime(runtime.getUptime() / 1000);

        var heap = new JvmInfoVO.Heap();
        heap.setUsed(memory.getHeapMemoryUsage().getUsed());
        heap.setCommitted(memory.getHeapMemoryUsage().getCommitted());
        heap.setMax(memory.getHeapMemoryUsage().getMax());

        var nonHeap = new JvmInfoVO.NonHeap();
        nonHeap.setUsed(memory.getNonHeapMemoryUsage().getUsed());

        var thread = new JvmInfoVO.Thread();
        thread.setCount(threads.getThreadCount());

        var os = new JvmInfoVO.Os();
        os.setAvailableProcessors(rt.availableProcessors());
        os.setTotalMemory(rt.totalMemory());
        os.setFreeMemory(rt.freeMemory());

        var vo = new JvmInfoVO();
        vo.setSource(source);
        vo.setJvm(jvm);
        vo.setHeap(heap);
        vo.setNonHeap(nonHeap);
        vo.setThread(thread);
        vo.setOs(os);
        return vo;
    }
}
