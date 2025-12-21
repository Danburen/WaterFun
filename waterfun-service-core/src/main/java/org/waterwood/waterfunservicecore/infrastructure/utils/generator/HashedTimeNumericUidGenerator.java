package org.waterwood.waterfunservicecore.infrastructure.utils.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.waterwood.utils.UidGenerator;
import org.waterwood.utils.codec.HashUtil;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * A Hashed Time Numeric Uid Generator
 * Generate like prefix + hashed digests(current timestamp - epoch) + timestamp(fall back filling only).
 */
@Component
public class HashedTimeNumericUidGenerator implements UidGenerator {
    @Value("${generator.uid.epoch:1735689600000}")
    private long EPOCH; // GMT: 2025-01-01 00:00:00
    @Value("${generator.uid.length:16}")
    private int LENGTH;
    @Value("${generator.uid.prefix:0}")
    private int prefix;
    private static final SecureRandom random = new SecureRandom();
    @Override
    public Long generateUid(){
        return generateUid(LENGTH);
    }

    @Override
    public Long generateUid(int length) {
        long timestamp = System.currentTimeMillis() - EPOCH;
        String input = timestamp + "-" + random.nextLong();

        byte[] hash = HashUtil.getSHA256Digest().digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        result.append(prefix);
        // Turn hashed bytes into a numeric string
        for (int i = 0; i < Math.min(length, hash.length); i++) {
            result.append(Math.abs(hash[i]) % 10);
        }
        // Use timestamp as fallback
        while (result.length() < length) {
            result.append(Math.abs(timestamp % 10));
            timestamp /= 10;
        }
        return Long.parseLong(result.substring(0, length));
    }
}
