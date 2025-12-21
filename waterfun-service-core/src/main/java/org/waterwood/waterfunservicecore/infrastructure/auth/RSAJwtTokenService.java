package org.waterwood.waterfunservicecore.infrastructure.auth;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.waterfunservicecore.infrastructure.security.RsaJwtUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RSAJwtTokenService implements AuthTokenService {
    private final RsaJwtUtil rsaJwtUtil;
    private final RedisHelper redisHelper;

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
        redisHelper.setKeyPrefix(REDIS_TOKEN_KEY_PREFIX);
        this.deviceService = deviceService;
    }

    @Override
    public TokenResult generateStoreNewAndRevokeOthers(Long userUid, String deviceId) {
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.SUBJECT,String.valueOf(userUid));
        claims.put(Claims.ID,jti);
        claims.put("did", deviceId);

        Duration expire = Duration.ofSeconds(accessTokenExpire);
        TokenResult result = rsaJwtUtil.generateToken(claims,expire);
        // Store the access token jti to redis repository
        redisHelper.set(redisHelper.buildKeys(ACCESS_TOKEN_JTI, userUid.toString(),deviceId),jti,expire);

        // Revoke other access tokens and device
        List<String>  userDevices = deviceService.getUserDeviceIds(userUid);
        for (String did : userDevices) {
            if(did.equals(deviceId)) continue; // skip current device
            redisHelper.del(redisHelper.buildKeys(ACCESS_TOKEN_JTI, userUid.toString(),did));
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
    public TokenResult generateAndStoreRefreshToken(long userUid, String deviceId, long expireInSeconds) {
        String refreshToken = UUID.randomUUID().toString();
        redisHelper.set(redisHelper.buildKeys(REFRESH_TOKEN_KEY,refreshToken),
                gson.toJson(Map.of("userUid",userUid,
                        "did",deviceId)),
                Duration.ofSeconds(expireInSeconds));
//        log.info("Refresh token: {}", refreshToken);
        return new TokenResult(refreshToken,expireInSeconds);
    }

    @Override
    public TokenResult generateAndStoreRefreshToken(long userUid, String deviceId) {
        return generateAndStoreRefreshToken(userUid,deviceId,refreshTokenExpire);
    }

    @Override
    public TokenResult RegenerateRefreshToken(String oldRefreshToken, long userUid, String deviceId) {
        long restExpire = redisHelper.getExpire(redisHelper.buildKeys(REFRESH_TOKEN_KEY,oldRefreshToken));
        return generateAndStoreRefreshToken(userUid,deviceId,restExpire);
    }

    /**
     * Validates the refresh tokenValue and returns the userUid if valid.
     * <p><b>Refresh Token will be removed </b>after validateAndRemove</p>
     * @param refreshToken the refresh tokenValue to validateAndRemove
     * @return Long of <b>UserID</b> if the tokenValue is valid
     */
    @Override
    public RefreshTokenPayload validateRefreshToken(String refreshToken, String dfp) {
        String key = redisHelper.buildKeys(REFRESH_TOKEN_KEY,refreshToken);
        String jsonRes = redisHelper.getValue(key);
        if (jsonRes == null) { // MISSING Refresh token
            throw new BusinessException(BaseResponseCode.REAUTHENTICATE_REQUIRED);
        }
        long userUid = Double.valueOf((double)gson.fromJson(jsonRes, Map.class).get("userUid")).longValue();
        String originalDid = (String) gson.fromJson(jsonRes, Map.class).get("did");
        String did = deviceService.generateDeviceId(userUid,dfp);
        if(! did.equals(originalDid)) { // Device Fingerprint changed
            log.info("User ID: {} , device Fingerprint changed: {} -> {}",userUid,originalDid,dfp);
        }
        return new RefreshTokenPayload(userUid,did);
    }

    @Override
    public void validateAccessTokenAndRejectOld(Claims claims) {
        String userUid = claims.getSubject();
        String iss = claims.getIssuer();
        String jti = claims.getId();
        String did = (String) claims.get("did");
        if(iss == null || !iss.equals(rsaJwtUtil.getIssuer())) throw new JwtException("Invalid issuer");
        String jtiKey = redisHelper.buildKeys(redisHelper.buildKeys(ACCESS_TOKEN_JTI, userUid,did));
        String savedJti = redisHelper.getValue(jtiKey);
        if(savedJti == null || !savedJti.equals(jti)){
            throw new JwtException("Invalid token ID");
        }
    }

    @Override
    public Claims parseToken(String ValidatedAccessToken) throws JwtException{
        return rsaJwtUtil.parseToken(ValidatedAccessToken);
    }

    @Override
    public void removeRefreshToken(String refreshToken) {
        redisHelper.del(redisHelper.buildKeys(REFRESH_TOKEN_KEY, refreshToken));
    }

    @Override
    public void removeAccessToken(Long userUid, String deviceId) {
        redisHelper.del(redisHelper.buildKeys(ACCESS_TOKEN_JTI, userUid.toString(),deviceId));
    }

    @Override
    public Long getCurrentUserUid() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        return Long.parseLong(jwt.getSubject());
    }
}
