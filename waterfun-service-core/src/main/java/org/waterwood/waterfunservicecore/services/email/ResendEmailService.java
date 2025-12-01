package org.waterwood.waterfunservicecore.services.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
public class ResendEmailService extends EmailServiceBase {
    @Value("${mail.resend.api-key}")
    private String apiKey;

    private final Resend resend;
    protected ResendEmailService(SpringTemplateEngine templateEngine) {
        super(templateEngine);
        this.resend = new Resend(apiKey);
    }


    @Override
    public EmailCodeResult sendHtmlEmail(String to, String from, String subject, String html) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(html)
                .build();
        return sendEmail(params,to);
    }

    @Override
    public EmailCodeResult sendSimpleEmail(String to, String from, String subject, String text) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .text(text)
                .build();
        return sendHtmlEmail(to, from, subject, text);
    }

    private EmailCodeResult sendEmail(CreateEmailOptions params,String to) {
        try{
            CreateEmailResponse res = resend.emails().send(params);
            return EmailCodeResult.builder()
                    .sendSuccess(true)
                    .email(to)
                    .responseRaw(res.getId())
                    .build();
        } catch (ResendException e) {
            EmailCodeResult result = EmailCodeResult.builder()
                    .sendSuccess(false)
                    .email(to)
                    .message("Email send fail,Please check the email provider & params.")
                    .build();
            log.error(result.getMessage(), e);
            return result;
        }
    }
}
