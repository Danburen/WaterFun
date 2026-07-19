package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunadminservice.api.response.AuditLogResponse;
import org.waterwood.waterfunservicecore.entity.audit.AuditLog;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.AuditLogRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Override
    public Page<AuditLogResponse> listAuditLogs(Specification<AuditLog> spec, Pageable pageable) {
        return auditLogRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Override
    public AuditLogResponse getAuditLog(Long id) {
        return toResponse(auditLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("AuditLog ID: " + id)));
    }

    @Transactional
    @Override
    public void deleteAuditLog(Long id) {
        auditLogRepository.deleteById(id);
    }

    @Transactional
    @Override
    public BatchResult deleteAuditLogs(List<Long> ids) {
        int removed = 0;
        if (CollectionUtil.isNotEmpty(ids)) {
            auditLogRepository.deleteAllById(ids);
            removed = ids.size();
        }
        return BatchResult.ofNullable(ids, removed);
    }

    /**
     * Map AuditLog entity to AuditLogResponse DTO.
     */
    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse resp = new AuditLogResponse();
        resp.setId(log.getId());
        resp.setUserId(log.getUserId());
        resp.setUsername(log.getUsername());
        resp.setAction(log.getAction());
        resp.setIp(log.getIp());
        resp.setDeviceInfo(log.getDeviceInfo());
        resp.setCountry(log.getCountry());
        resp.setProvince(log.getProvince());
        resp.setCity(log.getCity());
        resp.setStatus(log.getStatus());
        resp.setFailReason(log.getFailReason());
        resp.setCreatedAt(log.getCreatedAt());
        return resp;
    }
}
