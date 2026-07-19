package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunadminservice.api.response.IpAccessLogResponse;
import org.waterwood.waterfunservicecore.entity.security.IpAccessLog;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.IpAccessLogRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IpAccessLogServiceImpl implements IpAccessLogService {

    private final IpAccessLogRepository ipAccessLogRepository;

    @Override
    public Page<IpAccessLogResponse> listIpAccessLogs(Specification<IpAccessLog> spec, Pageable pageable) {
        return ipAccessLogRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Override
    public IpAccessLogResponse getIpAccessLog(Long id) {
        return toResponse(ipAccessLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IpAccessLog ID: " + id)));
    }

    private IpAccessLogResponse toResponse(IpAccessLog log) {
        IpAccessLogResponse resp = new IpAccessLogResponse();
        resp.setId(log.getId());
        resp.setIp(log.getIp());
        resp.setRequestPath(log.getRequestPath());
        resp.setRequestMethod(log.getRequestMethod());
        resp.setUserUid(log.getUserUid());
        resp.setHttpStatus(log.getHttpStatus());
        resp.setCountry(log.getCountry());
        resp.setProvince(log.getProvince());
        resp.setCity(log.getCity());
        resp.setCreatedAt(log.getCreatedAt());
        return resp;
    }
}
