package org.waterwood.waterfunadminservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.service.EncryptionAdminService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;

@RestController
@RequestMapping("/api/admin/encryption")
@RequiredArgsConstructor
public class EncryptionController {

    private final EncryptionAdminService encryptionAdminService;

    @PostMapping("/rotate")
    public ApiResponse<EncryptedKeyService.KeyRotationResult> rotateKeys() {
        EncryptedKeyService.KeyRotationResult result = encryptionAdminService.rotateKeys();
        return ApiResponse.success(result);
    }
}
