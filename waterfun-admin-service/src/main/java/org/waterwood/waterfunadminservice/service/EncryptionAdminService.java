package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;

@Service
@RequiredArgsConstructor
public class EncryptionAdminService {

    private final EncryptedKeyService encryptedKeyService;

    public EncryptedKeyService.KeyRotationResult rotateKeys() {
        return encryptedKeyService.rotateAesKey();
    }
}
