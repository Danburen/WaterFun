package org.waterwood.waterfunservicecore.services.auth.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.waterfunservicecore.infrastructure.security.RsaJwtUtil;
import org.waterwood.waterfunservicecore.services.auth.AuthTokenService;
import org.waterwood.waterfunservicecore.services.auth.DeviceService;
import org.waterwood.common.constratin.UserKeyBuilder;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class TokenService implements AuthTokenService {
    private final RsaJwtUtil rsaJwtUtil;
    private final RedisHelperHolder redisHelper;
    private final DeviceService deviceService;


    @Value("${token.refresh.rotate:604800}") // Default to 7 days in seconds
    private Long refRotateExpire;
    @Value("${token.refresh.family:2592000}") // Default to 30 days in seconds
    private Long refFamilyExpire;
    @Value("${token.access.expiration:3600}") // Default to 1 hour in seconds
    private Long accessTokenExpire;
    public TokenService(RedisHelper redisHelper, RsaJwtUtil rsaJwtUtil, DeviceServiceImpl deviceService) {
        this.redisHelper = redisHelper;
        this.rsaJwtUtil = rsaJwtUtil;
        this.deviceService = deviceService;
    }

    @Override
    public TokenResult genCacheNewAccTokenRevokeOlds(Long userUid, String deviceId) {
        String jti = UUID.randomUUID().toString();
        Map<String, String> claims = new HashMap<>();
        claims.put(Claims.SUBJECT,String.valueOf(userUid));
        claims.put(Claims.ID,jti);
        claims.put("did", deviceId);

        Duration expire = Duration.ofSeconds(accessTokenExpire);
        TokenResult result = rsaJwtUtil.generateToken(claims,expire);
        // Store the access token jti to redis repository
        redisHelper.set(buildAccessUserDeviceKey(userUid, deviceId), jti, expire);
        deviceService.updateUserDeviceActive(userUid, deviceId);
        return result;
    }

    @Override
    public TokenResult genAndCacheRefToken(long userUid, String deviceId) {
        String family = redisHelper.getValue(buildRtFamilyCacheKey(userUid,deviceId));
        if(family == null) { // no family ,we create a new one
            family = generateFamilyId();
            redisHelper.set(buildRtFamilyCacheKey(userUid, deviceId), family, Duration.ofSeconds(refFamilyExpire));
            redisHelper.sAdd(buildRtFamiliesCacheKey(userUid), family, String.valueOf(System.currentTimeMillis()));
        }
        String RT = UUID.randomUUID().toString();
        redisHelper.set(buildRefCacheKey(userUid, deviceId, family) , RT, Duration.ofSeconds(refRotateExpire));
        return new TokenResult(RT,refRotateExpire);
    }

    /**
     * Validates the refresh tokenValue and returns the userUid if valid.
     * <p><b>Refresh Token will be removed </b>after validateAndRemove</p>
     *
     * @param userUid the user UID
     * @param refreshToken the refresh tokenValue to validateAndRemove
     * @return Long of <b>UserID</b> if the tokenValue is valid
     */
    @Override
    public RefreshTokenPayload validateRefreshToken(long userUid, String refreshToken, String dfp) {
        String calculatedHashDid = deviceService.calculaateDid(userUid,dfp);
        String familyId = redisHelper.getValue(buildRtFamilyCacheKey(userUid, calculatedHashDid));
        if(familyId == null) throw new BizException(BaseResponseCode.REAUTHENTICATE_REQUIRED); // INVALID Refresh token family
        boolean isNewDevice = deviceService.isNewDeviceDid(userUid, calculatedHashDid);
        if(isNewDevice){
            // TODO: Add device risk control;
            log.info("New device detected for user {}, did {}, calculatedDid {}, familyId {}", userUid, dfp, calculatedHashDid, familyId);
        }
        String ref = redisHelper.getValue(buildRefCacheKey(userUid, calculatedHashDid, familyId));
        if(ref == null){
            throw new BizException(BaseResponseCode.REAUTHENTICATE_REQUIRED); // Missing refresh token
        }
        return new RefreshTokenPayload(userUid,calculatedHashDid);
    }

    @Override
    public void validateAccessTokenAndRejectOld(Claims claims) {
        String userUid = claims.getSubject();
        String jti = claims.getId();
        String did = (String) claims.get("did");
        String jtiKey = buildAccessUserDeviceKey(Long.parseLong(userUid), did);
        String savedJti = redisHelper.getValue(jtiKey);
        if(savedJti == null || !savedJti.equals(jti)){
            throw new JwtException("Invalid token ID");
        }
    }

    @Override
    public void removeRefreshToken(long userUid, String dfp, String refreshToken) {
        String calculatedHashDid = deviceService.calculaateDid(userUid,dfp);
        String familyId = redisHelper.getValue(buildRtFamilyCacheKey(userUid, calculatedHashDid));
        redisHelper.del(buildRefCacheKey(userUid, calculatedHashDid, familyId));
    }

    @Override
    public void removeAccessToken(Long userUid, String deviceId) {
        redisHelper.del(buildAccessUserDeviceKey(userUid, deviceId));
    }
    @Override
    public String buildRefCacheKey(long userUid, String deviceId, String family) {
        return "user:"+ userUid + ":device:" + deviceId + ":rt_family:" + family + ":ref";
    }

    public String buildRtFamilyCacheKey(long userUid, String deviceId) {
        return "user:"+ userUid + ":device:" + deviceId + ":rt_family";
    }

    @Override
    public String buildRtFamiliesCacheKey(long userUid) {
        return "user:"+ userUid + ":rt_families";
    }

    @Override
    public String buildAccessUserDeviceKey(long userUid, String deviceId){
        return UserKeyBuilder.userAccessDevice(userUid, deviceId);
    }

    @Override
    public String generateFamilyId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public Set<String> getFamilyIds(long userUid) {
        return redisHelper.sMem(buildRtFamiliesCacheKey(userUid));
    }

    @Override
    public void cleanZombieRefFamily() {
        // TODO: add asynchronous remove to help release server pressure.
        ScanOptions options = ScanOptions.scanOptions()
                .match("user:*:rt_families")
                .count(100)  // 100 per batch
                .build();
        Cursor<String> cursor = redisHelper.scan(options);
        List<String> batch = new ArrayList<>();
        long removed = 0;
        long batchCount = 0;
        while(cursor.hasNext()){
            batch.add(cursor.next());
            if(batch.size() >= 100){
                removed += processBatchRTFamiliesClean(batch);
                batch.clear();
                batchCount++;
            }
        }
        if (!batch.isEmpty()) {
            removed += processBatchRTFamiliesClean(batch);
        }
        cursor.close();
        log.info("Zombie Refresh Families successfully cleaned up, total {} in {} batches", removed, batchCount);
    }

    private long processBatchRTFamiliesClean(List<String> batch) {
        long removed = 0;
        for(String key: batch){
            Set<String> familiesSet = redisHelper.sMem(key);
            if(familiesSet == null || familiesSet.isEmpty()) {
                redisHelper.del(key);
                continue;
            }
            List<String> toRemove = new ArrayList<>();
            List<Boolean> exists = redisHelper.hasKeys(familiesSet.stream().toList());
            Iterator<String> familyIter = familiesSet.iterator();
            Iterator<Boolean> existsIter = exists.iterator();

            while(familyIter.hasNext() && existsIter.hasNext()){
                String family = familyIter.next();
                boolean exist = existsIter.next();
                if(!exist){
                    toRemove.add(family);
                }
            }
            redisHelper.sRem(key, toRemove);
        }
        return removed;
    }
}
