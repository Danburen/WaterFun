package org.waterwood.waterfunservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservice.api.response.MiniFileResData;
import org.waterwood.waterfunservice.service.resource.LegalResourceConstants;
import org.waterwood.waterfunservice.service.resource.ResourceService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api/resource/")
@Slf4j
public class ResourceController {
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private CloudFileService cloudFileService;

    @GetMapping("legal/{type}/{lang}/{fileName}")
    public ApiResponse<MiniFileResData> getLegalResource(
            @PathVariable String type,
            @PathVariable String lang,
            @PathVariable String fileName) {
        if (!LegalResourceConstants.VALID_TYPES.contains(type)
                || !LegalResourceConstants.VALID_LANGS.contains(lang)) {
            return ApiResponse.error(BaseResponseCode.REQUEST_NOT_IN_WHITELIST);
        }
        // 受保护的资源类型需要认证才能访问（白名单模式）
        if (LegalResourceConstants.PROTECTED_TYPES.contains(type)
                && UserCtxHolder.getUserUid() == null) {
            return ApiResponse.error(BaseResponseCode.HTTP_UNAUTHORIZED);
        }
        if (!isSafePathSegment(type) || !isSafePathSegment(lang) || !isSafePathSegment(fileName)) {
            return ApiResponse.error(BaseResponseCode.INVALID_PATH);
        }
        try {
            MiniFileResData data = resourceService.getLegalFileContent(type, lang, fileName);
            return ApiResponse.success(data);
        } catch (IOException e) {
            return ApiResponse.error(BaseResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isSafePathSegment(String segment) {
        return segment != null && segment.matches("^(?!\\.\\.)[a-zA-Z0-9_\\-.]++$");
    }

    private String detectContentType(String fileName) throws IOException {
        return Optional.ofNullable(Files.probeContentType(Paths.get(fileName)))
                .orElse("text/plain");
    }
}