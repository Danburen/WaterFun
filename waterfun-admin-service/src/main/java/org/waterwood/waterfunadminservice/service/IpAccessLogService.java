package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunadminservice.api.response.IpAccessLogResponse;
import org.waterwood.waterfunservicecore.entity.security.IpAccessLog;

public interface IpAccessLogService {

    /**
     * List IP access logs with dynamic filters and pagination.
     */
    Page<IpAccessLogResponse> listIpAccessLogs(Specification<IpAccessLog> spec, Pageable pageable);

    /**
     * Get a single IP access log by ID.
     */
    IpAccessLogResponse getIpAccessLog(Long id);
}
