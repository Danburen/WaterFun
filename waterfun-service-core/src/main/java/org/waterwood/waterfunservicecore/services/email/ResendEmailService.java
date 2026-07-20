package org.waterwood.waterfunservicecore.services.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.waterwood.utils.MaskUtil;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

@Slf4j
@Service
public class ResendEmailService extends EmailServiceBase {
    private final Resend resend;
    @Value("${third-party.communication.mock}")
    private boolean mockMode;

    protected ResendEmailService(SpringTemplateEngine templateEngine, @Value("${mail.resend.api-key}") String apiKey) {
        super(templateEngine);
        this.resend = new Resend(apiKey);
    }


    @Override
    public CodeResult sendHtmlEmail(String to, String from, String subject, String html) {
        if (mockMode) {
            log.info("[MOCK] Skipping real email to {}, subject: {}", MaskUtil.maskEmail(to), subject);
            return CodeResult.builder()
                    .sendSuccess(true)
                    .target(to)
                    .channel(VerifyChannel.EMAIL)
                    .build();
        }
        return sendHtmlEmailReal(to, from, subject, html);
    }

    @Override
    public CodeResult sendSimpleEmail(String to, String from, String subject, String text) {
        if (mockMode) {
            log.info("[MOCK] Skipping real simple email to {}, subject: {}", to, subject);
            return CodeResult.builder()
                    .sendSuccess(true)
                    .target(to)
                    .channel(VerifyChannel.EMAIL)
                    .build();
        }
        return sendSimpleEmailReal(to, from, subject, text);
    }

    /**
     * Always sends real HTML email via Resend, bypassing mock mode.
     * Public for testing and scenarios that require actual delivery.
     */
    public CodeResult sendHtmlEmailReal(String to, String from, String subject, String html) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(html)
                .build();
        return sendEmailReal(params, to);
    }

    /**
     * Always sends real simple email via Resend, bypassing mock mode.
     * Public for testing and scenarios that require actual delivery.
     */
    public CodeResult sendSimpleEmailReal(String to, String from, String subject, String text) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .text(text)
                .build();
        return sendHtmlEmailReal(to, from, subject, text);
    }

    private CodeResult sendEmailReal(CreateEmailOptions params, String to) {
        try{
            CreateEmailResponse res = resend.emails().send(params);
            return  CodeResult.builder()
                    .sendSuccess(true)
                    .target(to)
                    .channel(VerifyChannel.EMAIL)
                    .build();
        } catch (ResendException e) {
            throw new ServiceException("Email send fail,Please check the email provider & params." + e.getMessage());
        }
    }
}
