package org.waterwood.waterfunservicecore.services.email;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.waterwood.utils.MaskUtil;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import static org.waterwood.common.RedisKeyPrefix.VERIFY;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.services.auth.code.CodeVerifier;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSender;
import org.waterwood.waterfunservicecore.services.email.template.EmailTemplateFragment;
import org.waterwood.waterfunservicecore.services.email.template.EmailTemplateLayout;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class EmailCodeService implements CodeVerifier, CodeSender {
    private final RedisHelperHolder redisHelper;
    private final MessageSource messageSource;
    @Value("${expiresIn.email.verify}")
    private Long expireDuration;
    @Value("${mail.support.email}")
    private String supportEmail;
    private final ResendEmailService emailService;

    // -- Redis key builders --

    private static String emailCodeKey(String target, String scene, String identifier) {
        return RedisKeyBuilder.build(VERIFY, "email", target, scene, identifier);
    }

    @Override
    public CodeResult sendCode(String target, VerifyScene scene) {
        String code = generateVerifyCode();
        String uuid = UUID.randomUUID().toString();
        Locale locale = Locale.getDefault();

        Map<String,Object> data = new HashMap<>();
        data.put("verificationCode",code);
        data.put("expireTime",expireDuration);
        data.put("supportEmail","support@mail.waterfun.top");
        data.put("action", messageSource.getMessage("action.verify", null, locale));
        EmailTemplateFragment fragment = EmailTemplateFragment.ofScene(scene);
        EmailTemplateLayout layout = EmailTemplateLayout.ofScene(scene);
        String subject = messageSource.getMessage(fragment.getSubject(), null, locale);

        CodeResult sendResult= emailService.sendHtmlEmail(target, layout.getDefaultFrom(),
                subject,
                layout.getTemplateKey(),
                fragment.getTemplateKey() ,
                data);
        sendResult.setKey(uuid);
        log.info("Send email code to {}, send result key{}, code:{}", MaskUtil.maskEmail(target), sendResult.getKey(), code);

        if (sendResult.isSendSuccess()){
            redisHelper.set(
                    emailCodeKey(target, scene.getValue(), uuid),
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
                emailCodeKey(target, scene.getValue(), key),
                code
        );
    }

    @Override
    public String generateVerifyCode() {
        return String.valueOf( ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
