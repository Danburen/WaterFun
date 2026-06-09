package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.AuthCode;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.LoginService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepo;
    private final TokenService tokenService;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final CaptchaServiceImpl captchaService;
    private final VerificationService verificationService;
    private final SiteStatisticRecorder siteStatisticRecorder;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public User login(PwdLoginReq body, String verifyUUIDKey){
        Optional<User> user = userRepo.findByUsername(body.getUsername());
        User u = user.map(uu -> {
            if(! captchaService.verifyCode(verifyUUIDKey, body.getCaptcha()))
                throw new AuthException(AuthCode.CAPTCHA_INVALID);
            if(! encoder.matches(body.getPassword(), uu.getPasswordHash()))
                throw new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT);
            if(uu.getPasswordHash() == null)
                throw new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT);
            return uu;
        }).orElseThrow(() -> new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT));
        siteStatisticRecorder.recordLogin();
        return u;
    }

    @Override
    public boolean logout(String refreshToken, String dfp) {
        long userUid = UserCtxHolder.getUserUid();
        RefreshTokenPayload payload = tokenService.validateRefreshToken(userUid, refreshToken, dfp);
        tokenService.removeAccessToken(payload.userUid(), payload.deviceId());
        tokenService.removeRefreshToken(userUid, dfp, refreshToken);
        return true;
    }

    @Override
    public User login(VerifyCodeDto dto, String codeKey) {
        EncryptionDataKey key= encryptedKeyService.pickEncryptionKey(1);
        if(dto.getScene() != VerifyScene.LOGIN){
            throw new AuthException(AuthCode.INVALID_VERIFY_SCENE);
        }
        UserDatum userDatum = null;
        VerifyChannel channel = dto.getChannel();
        if(channel == VerifyChannel.SMS){
            userDatum = userDatumRepo.findByPhoneHash(HashUtil.Sha256HmacString(dto.getTarget(),key.getEncryptedKey()))
                    .orElseThrow(() ->  new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT));
        }else if(channel == VerifyChannel.EMAIL){
            userDatum = userDatumRepo.findByEmailHash(HashUtil.Sha256HmacString(dto.getTarget(),key.getEncryptedKey()))
                    .orElseThrow(() ->  new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT));
        }
        verificationService.verifyCode(dto.getTarget(),dto.getScene(),channel,codeKey,dto.getCode());

        if(userDatum ==  null) throw new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT);
        siteStatisticRecorder.recordLogin();
        return userDatum.getUser();
    }
}
