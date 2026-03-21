package org.waterwood.waterfunservicecore.services.auth.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.waterwood.api.ApiResponse;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.api.TokenPair;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.AuthService;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSenderFactory;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final TokenService tokenService;
    private final DeviceServiceImpl deviceService;
    private final UserRepository userRepository;
    private final CodeSenderFactory codeSenderFactory;

    @Override
    public ApiResponse<LoginClientData> BuildLoginResponse( HttpServletResponse response, User user, String dfp) {
        TokenPair tokenPair = createNewTokens(user.getUid(), dfp);
        CookieUtil.setTokenCookie(response,tokenPair);
        response.setContentType("application/json");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("X-Content-Type-Options", "nosniff");response.setHeader("X-Frame-Options", "DENY");
        return  ApiResponse.success(new LoginClientData(tokenPair.accessToken(),tokenPair.accessExp()));
    }

    @Override
    public TokenPair createNewTokens(long userUid, String deviceFingerprint) {
        String deviceId = deviceService.generateAndStoreDeviceId(userUid, deviceFingerprint);
        TokenResult accessToken = tokenService.genCacheNewAccTokenRevokeOlds(userUid, deviceId);
        TokenResult refreshToken = tokenService.genAndCacheRefToken(userUid, deviceId);
        return new TokenPair(
                accessToken.tokenValue(), accessToken.expire(),
                refreshToken.tokenValue(), refreshToken.expire());
    }

    /**
     * Return the api response of refresh access tokenValue operation.
     * <p>for future extension or refactor , we temporarily use api response instead of OpResult</p>
     *
     * @param refreshToken refresh tokenValue
     * @return ServiceResult type Token result that contains tokenValue and expirations.
     */
    @Override
    public TokenResult refreshAccessToken(String refreshToken, String dfp) {
        StringUtil.isBlankThen(refreshToken, () -> {
            throw new AuthException(BaseResponseCode.REAUTHENTICATE_REQUIRED);
        });// Missing refresh token
        long userUid = UserCtxHolder.getUserUid();
        RefreshTokenPayload payload = tokenService.validateRefreshToken(userUid, refreshToken, dfp);
        String deviceId = payload.deviceId();
        return userRepository.findById(userUid).map(_ ->
                        tokenService.genAndCacheRefToken(userUid, deviceId))
                .orElseThrow(() -> new AuthException(BaseResponseCode.USER_NOT_FOUND));
    }
}
