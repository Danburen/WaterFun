package org.waterwood.waterfunadminservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.monitor.MergedJvmInfo;
import org.waterwood.waterfunadminservice.api.response.monitor.SystemInfoVO;
import org.waterwood.waterfunadminservice.service.MonitorService;
import org.waterwood.waterfunservicecore.dto.JvmInfoVO;
import org.waterwood.waterfunservicecore.services.monitor.JvmMonitorService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final JvmMonitorService jvmMonitorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @GetMapping("/system")
    public ApiResponse<SystemInfoVO> system() {
        SystemInfoVO vo = monitorService.collect();
        vo.setJvms(fetchMergedJvm());
        return ApiResponse.success(vo);
    }

    @GetMapping("/jvms")
    public ApiResponse<MergedJvmInfo> jvm() {
        return ApiResponse.success(fetchMergedJvm());
    }

    @SuppressWarnings("unchecked")
    private MergedJvmInfo fetchMergedJvm() {
        JvmInfoVO adminJvm = jvmMonitorService.collect("admin");

        JvmInfoVO userJvm = null;
        String error = null;
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(userServiceUrl)
                    .build();
            String raw = client.get()
                    .uri("/api/monitor/jvm")
                    .retrieve()
                    .body(String.class);
            log.debug("Raw user-service response: {}", raw);
            Map<String, Object> resp = objectMapper.readValue(raw, Map.class);
            if (resp != null && Boolean.TRUE.equals(resp.get("success"))) {
                userJvm = objectMapper.convertValue(resp.get("data"), JvmInfoVO.class);
            } else {
                log.warn("User-service response not successful: {}", raw);
            }
        } catch (Exception e) {
            log.error("Failed to fetch user-service JVM info", e);
            error = e.getMessage();
        }

        return new MergedJvmInfo(adminJvm, userJvm, error);
    }
}
