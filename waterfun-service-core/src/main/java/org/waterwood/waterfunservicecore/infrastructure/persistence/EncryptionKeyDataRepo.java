package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.KeyStatus;

public interface EncryptionKeyDataRepo extends JpaRepository<EncryptionDataKey,String> {
    long countEncryptionDataKeysByKeyStatus(KeyStatus keyStatus);
}
