package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunadminservice.api.response.IpBanResponse;
import org.waterwood.waterfunservicecore.entity.security.IpBan;

import java.time.Instant;

public interface IpBanService {

    /**
     * List IP ban records with dynamic filters and pagination.
     */
    Page<IpBanResponse> listIpBanResponse(Specification<IpBan> spec, Pageable pageable);

    /**
     * Get a single IP ban record by ID.
     * @throws org.waterwood.waterfunservicecore.exception.notfound.NotFoundException if not found
     */
    IpBanResponse getIpBan(Long id);

    /**
     * Ban an IP address.
     * @param ip        the IP to ban
     * @param reason    optional reason for the ban
     * @param expiresAt optional expiration time; defaults to 7 days if null
     * @return the created ban record
     */
    IpBanResponse banIp(String ip, String reason, Instant expiresAt);

    /**
     * Unban an IP address by setting its expiresAt to now.
     */
    void unbanIp(String ip);

    /**
     * Delete an IP ban record by ID.
     */
    void deleteIpBan(Long id);
}
