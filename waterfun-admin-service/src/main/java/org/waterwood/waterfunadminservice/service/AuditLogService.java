package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.response.AuditLogResponse;
import org.waterwood.waterfunservicecore.entity.AuditLog;

import java.util.List;

public interface AuditLogService {
    Page<AuditLogResponse> listAuditLogs(Specification<AuditLog> spec, Pageable pageable);
    AuditLogResponse getAuditLog(Long id);
    void deleteAuditLog(Long id);
    BatchResult deleteAuditLogs(List<Long> ids);
}
