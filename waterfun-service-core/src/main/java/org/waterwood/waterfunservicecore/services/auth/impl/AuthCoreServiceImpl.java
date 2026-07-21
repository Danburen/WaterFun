package org.waterwood.waterfunservicecore.services.auth.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSenderFactory;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class AuthCoreServiceImpl implements AuthCoreService {
    private final AccessTokenServiceImpl authTokenServiceImpl;
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
        TokenResult accessToken = authTokenServiceImpl.genCacheNewAccTokenRevokeOlds(userUid, deviceId);
        TokenResult refreshToken = authTokenServiceImpl.genAndCacheRefToken(userUid, deviceId);
        return new TokenPair(
                accessToken.value(), accessToken.expiresIn(),
                refreshToken.value(), refreshToken.expiresIn());
    }

    /**
     * Return the refresh token
     *
     * @param refreshToken refresh value
     * @return Token result that contains value and expirations.
     */
    @Override
    public TokenPair refreshAccessToken(String refreshToken, String dfp) {
        StringUtil.isBlankThen(refreshToken, () -> {
            throw new AuthException(AuthCode.REAUTHORIZATION_REQUIRED);
        });// Missing refresh token
        // Resolve userUid from refresh token reverse index (not from access token)
        long userUid = authTokenServiceImpl.resolveUserUidByRefreshToken(refreshToken);
        RefreshTokenPayload payload = authTokenServiceImpl.validateRefreshToken(userUid, refreshToken, dfp);
        TokenResult RT = userRepository.findById(userUid).map(_ ->
                        authTokenServiceImpl.genAndCacheRefToken(userUid, payload.deviceId()))
                .orElseThrow(AuthException::new);
        TokenResult AT = authTokenServiceImpl.genCacheNewAccTokenRevokeOlds(userUid, payload.deviceId());
        return TokenPair.of(AT, RT);
    }
}
