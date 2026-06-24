package org.waterwood.waterfunservice.service.upload;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservice.api.UserBizType;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservice.service.report.ReportService;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadBizStrategy;

import java.util.List;
import java.util.Set;
@Component
@Getter
public class UserReportAttachmentStrategy implements UploadBizStrategy<UserUploadPolicyReq> {
    private final ReportService reportService;

    public UserReportAttachmentStrategy(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public Set<String> getTargetBizTypeCodes() {
        return Set.of(UserBizType.REPORT_ATTACHMENT_IMAGE.getCode());
    }

    @Override
    public List<PresignedResp> handle(UserUploadPolicyReq request) {
        return reportService.handleImageUpload(request);
    }

    @Override
    public String handleCallback(CloudPutCallbackReq request, BizUploadPayload payload) {
        reportService.handleImageCallback(
                request, payload.toContext(UserBizType.class, Long.class, UserUploadContext::new)
        );
        return payload.getResourceUuid();
    }


}
