package org.waterwood.waterfunservicecore.services.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.infrastructure.auth.RSAJwtTokenService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.api.req.auth.EmailLoginRequestBody;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginRequestBody;
import org.waterwood.waterfunservicecore.api.req.auth.SmsLoginRequestBody;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.services.email.EmailCodeService;

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

    public LoginServiceImpl(UserRepository ur, AuthServiceImpl as, RSAJwtTokenService ts, CaptchaServiceImpl cs, EncryptedKeyService edks, UserDatumRepo udr, EmailCodeService emailCodeService, SmsCodeService smsCodeService) {
        this.userRepo = ur;
        this.authService = as;
        this.tokenService = ts;
        this.captchaService = cs;
        this.encryptedKeyService = edks;
        this.userDatumRepo = udr;
        this.emailCodeService = emailCodeService;
        this.smsCodeService = smsCodeService;
    }



    @Override
    public User login(PwdLoginRequestBody body, String verifyUUIDKey){
        Optional<User> user = userRepo.findByUsername(body.getUsername());
        return user.map(u->{
            if(! encoder.matches(body.getPassword(), u.getPasswordHash())){
                throw new AuthException(BaseResponseCode.USERNAME_OR_PASSWORD_INCORRECT);
            }
            if(! captchaService.validateCaptcha(verifyUUIDKey,body.getCaptcha())){
                throw new AuthException(BaseResponseCode.CAPTCHA_INCORRECT);
            }
            return u;
        }).orElseThrow(()-> new ServiceException("User not found"));
    }

    @Override
    public User login(SmsLoginRequestBody body, String verifyUUIDKey){
        String phone = body.getPhoneNumber();
        EncryptionDataKey key= encryptedKeyService.pickEncryptionKey(1)
                .orElseThrow(() -> new ServiceException("Couldn't pick encryption key"));
        UserDatum datum = userDatumRepo.findByPhoneHash(HashUtil.Sha256HmacString(phone,key.getEncryptedKey()))
                .orElseThrow(() ->  new BusinessException(BaseResponseCode.PASSWORD_EMPTY_OR_INVALID));
        boolean verified = smsCodeService.verifySmsCode(phone,verifyUUIDKey,body.getSmsCode());
        if(verified){
            return datum.getUser();
        }else{
            throw new BusinessException(BaseResponseCode.SMS_CODE_INCORRECT);
        }
    }

    @Override
    public User login(EmailLoginRequestBody body, String verifyUUIDKey){
        String email = body.getEmail();
        EncryptionDataKey key= encryptedKeyService.pickEncryptionKey(1)
                .orElseThrow(() -> new ServiceException("Couldn't pick encryption key"));
        UserDatum userDatum = userDatumRepo.findByEmailHash(HashUtil.Sha256HmacString(email,key.getEncryptedKey()))
                .orElseThrow(() ->  new BusinessException(BaseResponseCode.USERNAME_OR_PASSWORD_INCORRECT));
        boolean verified = emailCodeService.verifyEmailCode(email,verifyUUIDKey,body.getEmailCode());
        if(verified){
            return userDatum.getUser();
        }else{
            throw new BusinessException(BaseResponseCode.EMAIL_CODE_INCORRECT);
        }
    }
    @Override
    public boolean logout(String refreshToken, String dfp) {
        RefreshTokenPayload payload = tokenService.validateRefreshToken(refreshToken,dfp);
            tokenService.removeAccessToken(payload.userId(), payload.deviceId());
            tokenService.removeRefreshToken(refreshToken);
        return true;
    }
}
