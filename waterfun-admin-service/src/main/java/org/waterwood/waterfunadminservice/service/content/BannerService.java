package org.waterwood.waterfunadminservice.service.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunadminservice.api.request.content.CreateBannerRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutBannerRequest;
import org.waterwood.waterfunadminservice.api.response.content.BannerResponse;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.Banner;

public interface BannerService {
    Page<Banner> list(Specification<Banner> spec, Pageable pageable);

    Banner getById(Long id);

    void createCallback(CreateBannerRequest req);

    void update(Long id, PutBannerRequest req);

    /**
     * Get banner upload coverage presigned url
     * @param suffix suffix of coverage file
     * @return Presgned resp
     */
    PresignedResp getCoverageUploadPolicy(String suffix);

    /**
     * Get banner by id with coverage url and assembled fields
     * @param id target banner id
     * @return banner response entity with cloud file presigned url to shown coverage.
     */
    BannerResponse getBanner(Long id);
}
