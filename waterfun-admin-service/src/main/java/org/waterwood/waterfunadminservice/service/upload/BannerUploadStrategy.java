package org.waterwood.waterfunadminservice.service.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunadminservice.api.AdminUploadContext;
import org.waterwood.waterfunadminservice.api.request.AdminUploadPolicyReq;
import org.waterwood.waterfunadminservice.service.content.AdminBizType;
import org.waterwood.waterfunadminservice.service.content.BannerService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadBizStrategy;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BannerUploadStrategy implements UploadBizStrategy<AdminUploadPolicyReq> {

    private final BannerService bannerService;

    @Override
    public Set<String> getTargetBizTypeCodes() {
        return Set.of(
                AdminBizType.BANNER_IMAGE.getCode()
        );
    }

    @Override
    public List<PresignedResp> handle(AdminUploadPolicyReq request) {
        return bannerService.handleBannerUpload(request);
    }

    @Override
    public void handleCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
            bannerService.handleBannerUploadCallback(request, payload.toContext(
                    AdminBizType.class,
                    Long.class,
                    AdminUploadContext::new
            ));
    }
}
