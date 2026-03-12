package org.waterwood.waterfunservicecore.services.auth.impl;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.cache.RedisHelperHolder;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.waterfunservicecore.infrastructure.security.RsaJwtUtil;
import org.waterwood.waterfunservicecore.services.auth.AuthTokenService;
import org.waterwood.waterfunservicecore.services.auth.DeviceService;
import org.waterwood.waterfunservicecore.services.auth.UserKeyBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RSAJwtTokenService implements AuthTokenService {
    private final RsaJwtUtil rsaJwtUtil;
    private final RedisHelperHolder redisHelper;

    private static final String REDIS_TOKEN_KEY_PREFIX = "token";
    private static final String REFRESH_TOKEN_KEY = "ref";
    private static final String ACCESS_TOKEN_JTI = "jti";

    private final Gson gson = new Gson();
    private final DeviceService deviceService;


    @Value("${token.refresh.expiration:604800}") // Default to 7 days in seconds
    private Long refreshTokenExpire;
    @Value("${token.access.expiration:3600}") // Default to 1 hour in seconds
    private Long accessTokenExpire;
    public RSAJwtTokenService(RedisHelper redisHelper, RsaJwtUtil rsaJwtUtil, DeviceServiceImpl deviceService) {
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
        redisHelper.set(buildAccCacheKey(userUid, deviceId) ,jti,expire);
        // Revoke other access tokens and device
        List<String>  userDevices = deviceService.getUserDeviceIds(userUid);
        for (String did : userDevices) {
            if(did.equals(deviceId)) continue; // skip current device
            redisHelper.del(buildRefCacheKey(userUid,did));
            deviceService.removeUserDevice(userUid,did);
        }
        return result;
    }

    /**
     * Generate and store refresh token
     * @param userUid the user ID
     * @param deviceId the identify of device
     * @param expireInSeconds expiration in seconds
     * @return Token result
     */
    @Override
    public TokenResult genAndCacheRefToken(long userUid, String deviceId, long expireInSeconds) {
        String refreshToken = UUID.randomUUID().toString();
        redisHelper.set(buildRefCacheKey(userUid, deviceId), deviceId, Duration.ofSeconds(expireInSeconds));
//        log.info("Refresh token: {}", refreshToken);
        return new TokenResult(refreshToken,expireInSeconds);
    }

    @Override
    public TokenResult genAndCacheRefToken(long userUid, String deviceId) {
        return genAndCacheRefToken(userUid,deviceId,refreshTokenExpire);
    }

    @Override
    public TokenResult ReGenRefreshToken(String oldRefreshToken, long userUid, String deviceId) {
        long restExpire = redisHelper.getExpire(buildRefCacheKey(userUid, oldRefreshToken));
        return genAndCacheRefToken(userUid,deviceId,restExpire);
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
        String key = buildRefCacheKey(userUid, refreshToken);
        String originalDid = redisHelper.getValue(key);
        if (originalDid == null) { // MISSING Refresh token
            throw new BizException(BaseResponseCode.REAUTHENTICATE_REQUIRED);
        }
        String did = deviceService.generateDeviceId(userUid,dfp);
        if(! did.equals(originalDid)) { // Device Fingerprint changed
            log.info("User ID: {} , device Fingerprint changed: {} -> {}",userUid,originalDid,dfp);
        }
        return new RefreshTokenPayload(userUid,did);
    }

    @Override
    public void validateAccessTokenAndRejectOld(Claims claims) {
        String userUid = claims.getSubject();
        String jti = claims.getId();
        String did = (String) claims.get("did");
        String jtiKey = buildAccCacheKey(Long.parseLong(userUid), did);
        String savedJti = redisHelper.getValue(jtiKey);
        if(savedJti == null || !savedJti.equals(jti)){
            throw new JwtException("Invalid token ID");
        }
    }

    @Override
    public void removeRefreshToken(long userUid, String refreshToken) {
        redisHelper.del(buildRefCacheKey(userUid, refreshToken));
    }

    @Override
    public void removeAccessToken(Long userUid, String deviceId) {
        redisHelper.del(buildAccCacheKey(userUid, deviceId));
    }
    @Override
    public String buildRefCacheKey(long userUid, String refToken) {
        return UserKeyBuilder.userRefresh(userUid, refToken);
    }

    @Override
    public String buildAccCacheKey(long userUid, String deviceId){
        return UserKeyBuilder.userDevice(userUid, deviceId);
    }
}
