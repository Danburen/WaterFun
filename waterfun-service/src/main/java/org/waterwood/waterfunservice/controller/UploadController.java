package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservice.service.upload.UploadStrategyFactory;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.UploadCallbackResp;
import org.waterwood.waterfunservicecore.infrastructure.aspect.BanCheck;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {
    private final UploadStrategyFactory uploadStrategyFactory;
    private final CloudFileService cloudFileService;

    @BanCheck("ban:upload")
    @RateLimit(key = "avatarUpload", permits = 5)
    @PostMapping("/policy")
    public ApiResponse<List<PresignedResp>> getUploadPolity(@RequestBody @Valid UserUploadPolicyReq request) {
        return ApiResponse.success(uploadStrategyFactory.getStrategy(request.getBizType()).handle(request));
    }

    @PostMapping("/callback")
    public ApiResponse<UploadCallbackResp> uploadCallback(@RequestBody @Valid CloudPutCallbackReq request) {
        BizUploadPayload payload = cloudFileService.parseToken(request.getToken());
        return ApiResponse.success(
                new UploadCallbackResp(uploadStrategyFactory.getStrategy(payload).handleCallback(request, payload))
        );
    }
}
