package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunadminservice.api.response.IpBanResponse;
import org.waterwood.waterfunservicecore.entity.IpBan;

public interface IpBanService {
    Page<IpBanResponse> listIpBanResponse(Specification<IpBan> spec, Pageable pageable);
}
