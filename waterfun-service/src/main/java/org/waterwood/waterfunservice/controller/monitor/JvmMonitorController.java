package org.waterwood.waterfunservice.controller.monitor;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservicecore.dto.JvmInfoVO;
import org.waterwood.waterfunservicecore.services.monitor.JvmMonitorService;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class JvmMonitorController {

    private final JvmMonitorService jvmMonitorService;

    @GetMapping("/jvm")
    public ApiResponse<JvmInfoVO> jvm() {
        return ApiResponse.success(jvmMonitorService.collect("user"));
    }
}
