package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.request.AdminUploadPolicyReq;
import org.waterwood.waterfunadminservice.service.upload.AdminUploadStrategyFactory;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class UploadController {
    private final AdminUploadStrategyFactory factory;
    private final CloudFileService cloudFileService;

    @RateLimit(key = "adminUpload", permits = 5)
    @PostMapping("/policy")
    public ApiResponse<List<PresignedResp>> getUploadPolity(@RequestBody @Valid AdminUploadPolicyReq request) {
        return ApiResponse.success(factory.getStrategy(request.getBizType()).handle(request));
    }

    @PostMapping("/callback")
    public ApiResponse<Void> uploadCallback(@RequestBody @Valid CloudPutCallbackReq request) {
        BizUploadPayload payload = cloudFileService.parseToken(request.getToken());
        factory.getStrategy(payload).handleCallback(request, payload);
        return ApiResponse.success();
    }
}
