package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.request.UploadPolicyReq;
import org.waterwood.waterfunservice.service.upload.UploadStrategyFactory;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {
    private final UploadStrategyFactory uploadStrategyFactory;
    private final CloudFileService cloudFileService;

    @RateLimit(key = "avatarUpload", permits = 5)
    @PostMapping("/policy")
    public ApiResponse<List<PresignedResp>> getUploadPolity(@RequestBody @Valid UploadPolicyReq request) {
        return ApiResponse.success(uploadStrategyFactory.getStrategy(request.getBizType()).handle(request));
    }

    @PostMapping("/callback")
    public ApiResponse<Void> uploadCallback(@RequestBody @Valid CloudPutCallbackReq request) {
        BizUploadPayload payload = cloudFileService.parseToken(request.getToken());
        uploadStrategyFactory.getStrategy(payload).handleCallback(request, payload);
        return ApiResponse.success();
    }
}
