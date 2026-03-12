package org.waterwood.waterfunservicecore.services.email;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.waterwood.common.cache.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.services.auth.VerifyKeyBuilder;
import org.waterwood.waterfunservicecore.services.auth.code.CodeVerifier;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSender;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class EmailCodeService implements CodeVerifier, CodeSender {
    private static final String REDIS_KEY_PREFIX = "verify:email-code";
    private final RedisHelperHolder redisHelper;
    private final MessageSource messageSource;
    @Value("${expire.email.verify}")
    private Long expireDuration;
    @Value("${mail.support.email}")
    private String supportEmail;
    private final ResendEmailService emailService;

    @Override
    public CodeResult sendCode(String target, VerifyScene scene) {
        String code = generateVerifyCode();
        String uuid = UUID.randomUUID().toString();
//        Locale locale = Locale.getDefault();
//
//        Map<String,Object> data = new HashMap<>();
//        data.put("verificationCode",code);
//        data.put("expireTime",expireDuration);
//        data.put("supportEmail","support@mail.waterfun.top");
//        data.put("action", messageSource.getMessage("action.verify", null, locale));
//        EmailTemplateFragment fragment = EmailTemplateFragment.ofScene(scene);
//        EmailTemplateLayout layout = EmailTemplateLayout.ofScene(scene);
//        String subject = messageSource.getMessage(fragment.getSubject(), null, locale);
//
//        CodeResult sendResult= emailService.sendHtmlEmail(target, layout.getDefaultFrom(),
//                subject,
//                layout.getTemplateKey(),
//                fragment.getTemplateKey() ,
//                data);
//        sendResult.setKey(uuid);
        CodeResult sendResult = new CodeResult(true, target, VerifyChannel.SMS , uuid);
        log.info("send result key{}, code:{}",  sendResult.getKey(), code);

        if (sendResult.isSendSuccess()){
            redisHelper.set(
                    RedisKeyBuilder.buildKey(VerifyKeyBuilder.email(target), scene.getValue(), uuid),
                    code,
                    Duration.ofMinutes(expireDuration));
        }else{
            throw new ServiceException("Email Send Failed");
        }
        return sendResult;
    }

    @Override
    public VerifyChannel channel() {
        return VerifyChannel.EMAIL;
    }

    @Override
    public boolean verifyCode(String target, VerifyScene scene, String key, String code) {
        return redisHelper.validateAndRemove(
                RedisKeyBuilder.buildKey(VerifyKeyBuilder.email(target), scene.getValue(), key),
                code
        );
    }

    @Override
    public String generateVerifyCode() {
        return String.valueOf( ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
