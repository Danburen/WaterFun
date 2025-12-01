package org.waterwood.waterfunservicecore.services.auth;

import org.waterwood.waterfunservicecore.api.req.auth.EmailLoginRequestBody;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginRequestBody;
import org.waterwood.waterfunservicecore.api.req.auth.SmsLoginRequestBody;
import org.waterwood.waterfunservicecore.entity.user.User;

public interface LoginService {
    User login(PwdLoginRequestBody body, String verifyUUIDKey);

    User login(SmsLoginRequestBody body, String verifyUUIDKey);

    User login(EmailLoginRequestBody body, String verifyUUIDKey);

    /*
        @Deprecated
        public  ServiceResult<LoginServiceResponse> verifySmsCodeLogin(SmsLoginRequestBody requestBody, String uuid) {
            String phone = requestBody.getPhoneNumber();
            String phonePrefix = PartialEncryptionHelper.getPhonePrefix(phone);
            if(!ValidateUtil.validatePhone(phone)) return ServiceResult.failure(ResponseCode.PHONE_NUMBER_EMPTY_OR_INVALID);
            return encryptedKeyService.pickEncryptionKey(1).map(
                    key-> userDatumRepo.findByPhonePrefixAndPhoneHash(phonePrefix, HashUtil.calculateHmac(phone,key.getEncryptedKey())).map(
                            userDatum -> userRepo.findById(userDatum.getId()).map(
                                            user-> AuthValidator.start()
                                                    .checkEmpty(requestBody.getPhoneNumber(),ResponseCode.USERNAME_EMPTY_OR_INVALID)
                                                    .checkEmpty(requestBody.getSmsCode(), ResponseCode.SMS_CODE_EMPTY)
                                                    .checkEmpty(requestBody.getDeviceFp(),ResponseCode.DEVICE_FINGERPRINT_REQUIRED)
                                                    .check(authService.getSmsCodeService()
                                                            .verifySmsCode(phone, uuid, requestBody.getSmsCode()), ResponseCode.SMS_CODE_INCORRECT)
                                                    .then(()-> ServiceResult.success(new LoginServiceResponse(user.getId())))
                                                    .buildResult())
                                    .orElse(ResponseCode.USER_NOT_FOUND.toServiceResult()))
                            .orElse(ServiceResult.failure(ResponseCode.INTERNAL_SERVER_ERROR)))
                    .orElse(ServiceResult.failure(ResponseCode.INTERNAL_SERVER_ERROR));
        }

        @Deprecated
        public  ServiceResult<LoginServiceResponse> verifyEmailLogin(EmailLoginRequestBody requestBody, String uuid) {
            String email = requestBody.getEmail();
            String emailDisplay = PartialEncryptionHelper.getEmailDisplay(email);
            if(!ValidateUtil.validateEmail(email)) return ServiceResult.failure(ResponseCode.EMAIL_ADDRESS_EMPTY_OR_INVALID);
            return encryptedKeyService.pickEncryptionKey(1).map(
                    key-> userDatumRepo.findByEmailDisplayAndEmailHash(emailDisplay,HashUtil.calculateHmac(email,key.getEncryptedKey())).map(
                            userDatum -> userRepo.findById(userDatum.getId()).map(
                                    user-> AuthValidator.start()
                                            .checkEmpty(requestBody.getEmail(),ResponseCode.EMAIL_ADDRESS_EMPTY_OR_INVALID)
                                            .checkEmpty(requestBody.getEmailCode(), ResponseCode.EMAIL_CODE_EMPTY)
                                            .checkEmpty(requestBody.getDeviceFp(),ResponseCode.DEVICE_FINGERPRINT_REQUIRED)
                                            .check(authService.getEmailCodeService().verifyEmailCode(
                                                    requestBody.getEmail(),uuid,requestBody.getEmailCode()), ResponseCode.EMAIL_CODE_INCORRECT)
                                            .then(()-> ServiceResult.success(new LoginServiceResponse(user.getId())))
                                            .buildResult()
                                    )
                                    .orElse(ResponseCode.USER_NOT_FOUND.toServiceResult()))
                            .orElseGet(ResponseCode.EMAIL_ADDRESS_EMPTY_OR_INVALID::toServiceResult))
                    .orElse(ServiceResult.failure(ResponseCode.INTERNAL_SERVER_ERROR));
        }*/
    boolean logout(String refreshToken, String dfp);
}
