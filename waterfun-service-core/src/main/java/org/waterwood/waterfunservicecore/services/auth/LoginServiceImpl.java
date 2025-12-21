package org.waterwood.waterfunservicecore.services.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.infrastructure.auth.RSAJwtTokenService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.email.EmailCodeService;
import org.waterwood.waterfunservicecore.services.sms.SmsCodeService;

import java.util.Optional;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepo;
    private final AuthServiceImpl authService;
    private final RSAJwtTokenService tokenService;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final CaptchaServiceImpl captchaService;
    private final EmailCodeService emailCodeService;
    private final SmsCodeService smsCodeService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final VerificationService verificationService;

    public LoginServiceImpl(UserRepository ur, AuthServiceImpl as, RSAJwtTokenService ts, CaptchaServiceImpl cs, EncryptedKeyService edks, UserDatumRepo udr, EmailCodeService emailCodeService, SmsCodeService smsCodeService, VerificationService verificationService) {
        this.userRepo = ur;
        this.authService = as;
        this.tokenService = ts;
        this.captchaService = cs;
        this.encryptedKeyService = edks;
        this.userDatumRepo = udr;
        this.emailCodeService = emailCodeService;
        this.smsCodeService = smsCodeService;
        this.verificationService = verificationService;
    }



    @Override
    public User login(PwdLoginReq body, String verifyUUIDKey){
        Optional<User> user = userRepo.findByUsername(body.getUsername());
        return user.map(u->{
            if(! encoder.matches(body.getPassword(), u.getPasswordHash())){
                throw new AuthException(BaseResponseCode.USERNAME_OR_PASSWORD_INCORRECT);
            }
            if(! captchaService.verifyCode(verifyUUIDKey,body.getCaptcha())){
                throw new AuthException(BaseResponseCode.CAPTCHA_INVALID);
            }
            // User didn't set password so they are not allow to log in by password.
            if( u.getPasswordHash() == null){
                throw new BusinessException(BaseResponseCode.FORBIDDEN);
            }
            return u;
        }).orElseThrow(()-> new ServiceException("User not found"));
    }

    @Override
    public boolean logout(String refreshToken, String dfp) {
        RefreshTokenPayload payload = tokenService.validateRefreshToken(refreshToken,dfp);
            tokenService.removeAccessToken(payload.userUid(), payload.deviceId());
            tokenService.removeRefreshToken(refreshToken);
        return true;
    }

    @Override
    public User login(VerifyCodeDto dto, String codeKey) {
        EncryptionDataKey key= encryptedKeyService.pickEncryptionKey(1);
        if(dto.getScene() != VerifyScene.LOGIN){
            throw new BusinessException(BaseResponseCode.INVALID_VERIFY_SCENE);
        }
        UserDatum userDatum = null;
        VerifyChannel channel = dto.getChannel();
        if(channel == VerifyChannel.SMS){
            userDatum = userDatumRepo.findByPhoneHash(HashUtil.Sha256HmacString(dto.getTarget(),key.getEncryptedKey()))
                    .orElseThrow(() ->  new BusinessException(BaseResponseCode.USERNAME_OR_PASSWORD_INCORRECT));
        }else if(channel == VerifyChannel.EMAIL){
            userDatum = userDatumRepo.findByEmailHash(HashUtil.Sha256HmacString(dto.getTarget(),key.getEncryptedKey()))
                    .orElseThrow(() ->  new BusinessException(BaseResponseCode.USERNAME_OR_PASSWORD_INCORRECT));
        }
        verificationService.verifyCode(dto.getTarget(),dto.getScene(),channel,codeKey,dto.getCode());

        if(userDatum ==  null) throw new BusinessException(BaseResponseCode.USERNAME_OR_PASSWORD_INCORRECT);
        return userDatum.getUser();
    }
}
