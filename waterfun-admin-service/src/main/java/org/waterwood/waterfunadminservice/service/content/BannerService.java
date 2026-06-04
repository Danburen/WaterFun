package org.waterwood.waterfunadminservice.service.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunadminservice.api.AdminUploadContext;
import org.waterwood.waterfunadminservice.api.request.AdminUploadPolicyReq;
import org.waterwood.waterfunadminservice.api.request.content.CreateBannerRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutBannerRequest;
import org.waterwood.waterfunadminservice.api.response.content.BannerResponse;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.Banner;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import java.util.List;

public interface BannerService {
    Page<Banner> list(Specification<Banner> spec, Pageable pageable);

    Banner getById(Long id);

    void createCallback(CreateBannerRequest req);

    void update(Long id, PutBannerRequest req);

    /**
     * Get banner by id with coverage url and assembled fields
     * @param id target banner id
     * @return banner response entity with cloud file presigned url to shown coverage.
     */
    BannerResponse getBanner(Long id);

    /**
     * Handle banner coverage upload and return presigned url for upload.
     * @param request upload policy request;
     * @return list of {@link PresignedResp} for banner coverage upload.
     */
    List<PresignedResp> handleBannerUpload(AdminUploadPolicyReq request);

    /**
     * Handle banner image upload callback
     *
     * @param request request body
     * @param ctx     payload suppose be parsed by {@link CloudFileService#parseToken(String)}
     */
    void handleBannerUploadCallback(CloudPutCallbackReq request, AdminUploadContext<Long> ctx);
}
