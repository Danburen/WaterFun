package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.response.AuditLogResponse;
import org.waterwood.waterfunservicecore.entity.audit.AuditLog;

import java.util.List;

public interface AuditLogService {

    /**
     * List audit logs with dynamic filters and pagination.
     */
    Page<AuditLogResponse> listAuditLogs(Specification<AuditLog> spec, Pageable pageable);

    /**
     * Get a single audit log by ID.
     * @throws org.waterwood.waterfunservicecore.exception.notfound.NotFoundException if not found
     */
    AuditLogResponse getAuditLog(Long id);

    /**
     * Delete a single audit log by ID.
     */
    void deleteAuditLog(Long id);

    /**
     * Batch delete audit logs by their IDs.
     * @return result summary of the batch operation
     */
    BatchResult deleteAuditLogs(List<Long> ids);
}
