package org.waterwood.waterfunservicecore.services.auth.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.waterwood.api.AuthCode;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.api.TokenPair;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSenderFactory;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthCoreServiceImpl implements AuthCoreService {
    private final TokenService tokenService;
    private final DeviceServiceImpl deviceService;
    private final UserRepository userRepository;
    private final CodeSenderFactory codeSenderFactory;

    @Override
    public LoginClientData BuildLoginResponse(HttpServletResponse response, User user, String dfp, Boolean isNewUser) {
        TokenPair tokenPair = createNewTokens(user.getUid(), dfp);
        CookieUtil.setTokenCookie(response,tokenPair);
        ResponseUtil.setNoCacheSecurityHeaders(response);
        return new LoginClientData(tokenPair.accessToken(), tokenPair.accessExp(), isNewUser);
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
     * Return the refresh token
     *
     * @param refreshToken refresh tokenValue
     * @return Token result that contains tokenValue and expirations.
     */
    @Override
    public TokenPair refreshAccessToken(String refreshToken, String dfp) {
        StringUtil.isBlankThen(refreshToken, () -> {
            throw new AuthException(AuthCode.REAUTHORIZATION_REQUIRED);
        });// Missing refresh token
        return UserCtxHolder.safeGetUserId().map(
                userUid -> {
                    RefreshTokenPayload payload = tokenService.validateRefreshToken(userUid, refreshToken, dfp);
                    TokenResult RT = userRepository.findById(userUid).map(_ ->
                                    tokenService.genAndCacheRefToken(userUid, payload.deviceId()))
                            .orElseThrow(AuthException::new);
                    TokenResult AT = tokenService.genCacheNewAccTokenRevokeOlds(userUid, payload.deviceId());
                    return TokenPair.of(AT, RT);
                }
        ).orElseThrow(() -> new AuthException(AuthCode.REAUTHORIZATION_REQUIRED));
    }
}
