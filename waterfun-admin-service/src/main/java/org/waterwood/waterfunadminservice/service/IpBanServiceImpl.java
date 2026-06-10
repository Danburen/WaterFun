package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunadminservice.api.response.IpBanResponse;
import org.waterwood.waterfunservicecore.entity.security.IpBan;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.IpBanRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IpBanServiceImpl implements IpBanService {
    private final IpBanRepository ipBanRepository;

    @Override
    public Page<IpBanResponse> listIpBanResponse(Specification<IpBan> spec, Pageable pageable) {
        return ipBanRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Override
    public IpBanResponse getIpBan(Long id) {
        return toResponse(ipBanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IpBan ID: " + id)));
    }

    @Override
    @Transactional
    public IpBanResponse banIp(String ip, String reason, Instant expiresAt) {
        IpBan ban = new IpBan();
        ban.setIp(ip);
        ban.setReason(reason != null ? reason : "");
        ban.setBannedAt(Instant.now());
        ban.setExpiresAt(expiresAt != null ? expiresAt : Instant.now().plusSeconds(86400 * 7));
        return toResponse(ipBanRepository.save(ban));
    }

    @Override
    @Transactional
    public void unbanIp(String ip) {
        ipBanRepository.unbanByIp(ip);
    }

    @Override
    @Transactional
    public void deleteIpBan(Long id) {
        ipBanRepository.deleteById(id);
    }

    /**
     * Map IpBan entity to IpBanResponse DTO.
     */
    private IpBanResponse toResponse(IpBan ban) {
        IpBanResponse resp = new IpBanResponse();
        resp.setId(ban.getId());
        resp.setIp(ban.getIp());
        resp.setReason(ban.getReason());
        resp.setBannedAt(ban.getBannedAt());
        resp.setExpiresAt(ban.getExpiresAt());
        return resp;
    }
}
