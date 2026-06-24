package org.waterwood.waterfunservicecore.infrastructure.security;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.EncryptionKeyDataRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages KEK & DEK encryption operations.
 * <p>
 * Three key purposes exist (each identified by {@code keyPurpose}):
 * <ul>
 *   <li><b>AES_DATA</b> — AES/GCM encrypt/decrypt phone/email fields</li>
 *   <li><b>USER_HMAC</b> — HMAC-SHA256 for phone/email dedup lookups</li>
 *   <li><b>VERIFY_HMAC</b> — HMAC-SHA256 for verify codes</li>
 * </ul>
 */
@Slf4j
@Service
public class EncryptedKeyService {
    public  static final String PURPOSE_AES_DATA   = "AES_DATA";
    public  static final String PURPOSE_USER_HMAC  = "USER_HMAC";
    public  static final String PURPOSE_VERIFY_HMAC = "VERIFY_HMAC";

    private final EncryptionKeyDataRepo encryptionKeyDataRepo;
    private final UserDatumRepo userDatumRepo;

    public EncryptedKeyService(EncryptionKeyDataRepo encryptionKeyDataRepo,
                               UserDatumRepo userDatumRepo) {
        this.encryptionKeyDataRepo = encryptionKeyDataRepo;
        this.userDatumRepo = userDatumRepo;
    }

    // ========== Initialization ==========

    @PostConstruct
    public void init(){
        migrateNullPurposeKeys();
        ensureMinKeysExists();
    }

    /** Assign purposes to legacy keys that have null keyPurpose. */
    private void migrateNullPurposeKeys() {
        List<EncryptionDataKey> nullPurposeKeys = encryptionKeyDataRepo.findByKeyPurposeIsNull();
        if (nullPurposeKeys.isEmpty()) return;
        String[] purposes = {PURPOSE_AES_DATA, PURPOSE_USER_HMAC, PURPOSE_VERIFY_HMAC};
        for (int i = 0; i < nullPurposeKeys.size() && i < purposes.length; i++) {
            nullPurposeKeys.get(i).setKeyPurpose(purposes[i]);
        }
        encryptionKeyDataRepo.saveAll(nullPurposeKeys);
        log.info("Migrated {} legacy keys with keyPurpose", nullPurposeKeys.size());
    }

    /** Ensure one ACTIVE key exists for each purpose. */
    private void ensureMinKeysExists(){
        ensureKeyForPurpose(PURPOSE_AES_DATA);
        ensureKeyForPurpose(PURPOSE_USER_HMAC);
        ensureKeyForPurpose(PURPOSE_VERIFY_HMAC);
    }

    private void ensureKeyForPurpose(String purpose) {
        Optional<EncryptionDataKey> existing = encryptionKeyDataRepo
                .findFirstByKeyPurposeAndKeyStatusOrderByCreatedAtDesc(purpose, KeyStatus.ACTIVE);
        if (existing.isPresent()) return;
        try {
            EncryptionDataKey key = EncryptionHelper.generateAndEncryptDEKs(1).getFirst();
            key.setKeyStatus(KeyStatus.ACTIVE);
            key.setKeyPurpose(purpose);
            encryptionKeyDataRepo.save(key);
            log.info("Generated new {} key", purpose);
        } catch (Exception e) {
            log.error("Failed to generate {} key: {}", purpose, String.valueOf(e));
        }
    }

    // ========== Key Lookup ==========

    public List<EncryptionDataKey> getAllKeys() {
        return encryptionKeyDataRepo.findAll();
    }

    /** Look up any key by its keyId (regardless of status). */
    public EncryptionDataKey getKeyById(String keyId) {
        return encryptionKeyDataRepo.findByKeyId(keyId)
                .orElseThrow(() -> new ServiceException("Encryption key not found: " + keyId));
    }

    /**
     * Return the most recent ACTIVE AES_DATA key (for new encryption).
     * During rotation the newest key should be used for encrypting new data.
     */
    public EncryptionDataKey getAesKey() {
        return encryptionKeyDataRepo
                .findFirstByKeyPurposeAndKeyStatusOrderByCreatedAtDesc(PURPOSE_AES_DATA, KeyStatus.ACTIVE)
                .orElseThrow(() -> new ServiceException("No active AES_DATA key found"));
    }

    /** Return the most recent ACTIVE USER_HMAC key. */
    public EncryptionDataKey getUserDatumHmacKey() {
        return encryptionKeyDataRepo
                .findFirstByKeyPurposeAndKeyStatusOrderByCreatedAtDesc(PURPOSE_USER_HMAC, KeyStatus.ACTIVE)
                .orElseThrow(() -> new ServiceException("No active USER_HMAC key found"));
    }

    /** Return the most recent ACTIVE VERIFY_HMAC key. */
    public EncryptionDataKey getVerifyHmacKey() {
        return encryptionKeyDataRepo
                .findFirstByKeyPurposeAndKeyStatusOrderByCreatedAtDesc(PURPOSE_VERIFY_HMAC, KeyStatus.ACTIVE)
                .orElseThrow(() -> new ServiceException("No active VERIFY_HMAC key found"));
    }

    // ========== Legacy positional lookups (retained for compatibility) ==========

    public Optional<EncryptionDataKey> randomPickEncryptionKey(){
        List<EncryptionDataKey> keys = getAllKeys();
        if(keys.isEmpty()){
            return Optional.empty();
        }else{
            return Optional.of(keys.get(ThreadLocalRandom.current().nextInt(keys.size())));
        }
    }

    public EncryptionDataKey pickEncryptionKey(int keyInd){
        List<EncryptionDataKey> keys = getAllKeys();
        if(keys.isEmpty()){
            throw new ServiceException("No active key found");
        }
        EncryptionDataKey encryptionKey = keys.stream()
                .filter(key -> key.getKeyStatus() == KeyStatus.ACTIVE)
                .toList()
                .get(keyInd);
        if(encryptionKey == null){
            throw new ServiceException("The key id: %d found".formatted(keyInd));
        }
        return encryptionKey;
    }

    public Optional<List<EncryptionDataKey>> pickEncryptionKeys(int... keysInd){
        List<EncryptionDataKey> keys = getAllKeys();
        if(keys.isEmpty()){
            return Optional.empty();
        }
        List<EncryptionDataKey> activeKeys = keys.stream()
                .filter(key -> key.getKeyStatus() == KeyStatus.ACTIVE)
                .toList();

        if (activeKeys.isEmpty()) {
            return Optional.empty();
        }
        if (keysInd == null || keysInd.length == 0) {
            return Optional.of(activeKeys);
        }
        List<EncryptionDataKey> selectedKeys = new ArrayList<>();
        for (int index : keysInd) {
            if (index >= 0 && index < activeKeys.size()) {
                selectedKeys.add(activeKeys.get(index));
            }
        }
        return selectedKeys.isEmpty() ? Optional.empty() : Optional.of(selectedKeys);
    }

    // ========== Management ==========

    private String generateKeyId() {
        return "dek-" + UUID.randomUUID();
    }

    @Transactional
    public Optional<EncryptionDataKey> updateKeyDescription(String keyId, String description) {
        return encryptionKeyDataRepo.findByKeyId(keyId)
                .map(key -> {
                    key.setDescription(description);
                    return encryptionKeyDataRepo.save(key);
                });
    }

    // ========== Key Rotation ==========

    /**
     * Generate a new AES_DATA key and re-encrypt all existing user data with it.
     * <p>
     * <b>Flow:</b>
     * <ol>
     *   <li>Create a new AES_DATA key, save as ACTIVE</li>
     *   <li>Iterate every {@link UserDatum}: decrypt phone/email with the old key,
     *       re-encrypt with the new key, update {@code encryptionKeyId}</li>
     *   <li>Transition the old AES_DATA key to {@link KeyStatus#DECRYPT_ONLY}</li>
     * </ol>
     * <p>
     * This method is idempotent — users already pointing to the new key are skipped.
     *
     * @return summary of the rotation result
     */
    @Transactional
    public KeyRotationResult rotateAesKey() {
        // ① Generate a new AES_DATA DEK
        EncryptionDataKey newKey;
        try {
            newKey = EncryptionHelper.generateAndEncryptDEKs(1).getFirst();
        } catch (Exception e) {
            throw new ServiceException("Failed to generate new DEK for rotation: " + e.getMessage());
        }
        newKey.setKeyStatus(KeyStatus.ACTIVE);
        newKey.setKeyPurpose(PURPOSE_AES_DATA);
        newKey.setDescription("Rotated AES Data Key " + Instant.now());
        encryptionKeyDataRepo.save(newKey);
        log.info("Created new AES_DATA key: {}", newKey.getKeyId());

        // ② Re-encrypt all user data
        int processed = 0;
        int skipped = 0;
        int errors = 0;

        List<UserDatum> allUsers = userDatumRepo.findAll();
        for (UserDatum ud : allUsers) {
            // Skip users already using the new key
            if (newKey.getKeyId().equals(ud.getEncryptionKeyId())) {
                skipped++;
                continue;
            }

            try {
                EncryptionDataKey oldKey = encryptionKeyDataRepo.findByKeyId(ud.getEncryptionKeyId())
                        .orElseThrow(() -> new ServiceException("Old key not found: " + ud.getEncryptionKeyId()));

                // Decrypt with old key
                String phone = ud.getPhoneEncrypted() != null
                        ? EncryptionHelper.decryptField(ud.getPhoneEncrypted(), oldKey)
                        : null;
                String email = ud.getEmailEncrypted() != null
                        ? EncryptionHelper.decryptField(ud.getEmailEncrypted(), oldKey)
                        : null;

                // Re-encrypt with new key
                if (phone != null) {
                    ud.setPhoneEncrypted(EncryptionHelper.encryptField(phone, newKey));
                }
                if (email != null) {
                    ud.setEmailEncrypted(EncryptionHelper.encryptField(email, newKey));
                }
                ud.setEncryptionKeyId(newKey.getKeyId());
                userDatumRepo.save(ud);
                processed++;
            } catch (Exception e) {
                log.error("Failed to re-encrypt user {}: {}", ud.getUid(), e.getMessage());
                errors++;
            }
        }

        // ③ Transition the previous AES_DATA ACTIVE key to DECRYPT_ONLY
        List<EncryptionDataKey> oldActiveAesKeys = encryptionKeyDataRepo
                .findByKeyPurposeAndKeyStatus(PURPOSE_AES_DATA, KeyStatus.ACTIVE);
        for (EncryptionDataKey key : oldActiveAesKeys) {
            if (!key.getKeyId().equals(newKey.getKeyId())) {
                key.setKeyStatus(KeyStatus.DECRYPT_ONLY);
                encryptionKeyDataRepo.save(key);
                log.info("Transitioned key {} to DECRYPT_ONLY", key.getKeyId());
            }
        }

        log.info("Key rotation complete: processed={}, skipped={}, errors={}", processed, skipped, errors);
        return new KeyRotationResult(newKey.getKeyId(), processed, skipped, errors);
    }

    /** Result summary of a key rotation operation. */
    public record KeyRotationResult(String newKeyId, int processed, int skipped, int errors) {}
}
