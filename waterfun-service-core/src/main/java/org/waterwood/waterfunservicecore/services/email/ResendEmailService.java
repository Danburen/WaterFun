package org.waterwood.waterfunservicecore.services.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

@Slf4j
@Service
public class ResendEmailService extends EmailServiceBase {


    private final Resend resend;
    protected ResendEmailService(SpringTemplateEngine templateEngine, @Value("${mail.resend.api-key}") String apiKey) {
        super(templateEngine);
        this.resend = new Resend(apiKey);
    }


    @Override
    public CodeResult sendHtmlEmail(String to, String from, String subject, String html) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(html)
                .build();
        return sendEmail(params,to);
    }

    @Override
    public CodeResult sendSimpleEmail(String to, String from, String subject, String text) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .text(text)
                .build();
        return sendHtmlEmail(to, from, subject, text);
    }

    private CodeResult sendEmail(CreateEmailOptions params,String to) {
        try{
            CreateEmailResponse res = resend.emails().send(params);
            return  CodeResult.builder()
                    .sendSuccess(true)
                    .target(to)
                    .channel(VerifyChannel.EMAIL)
                    .responseRaw(res.getId())
                    .build();
        } catch (ResendException e) {
            CodeResult result = CodeResult.builder()
                    .sendSuccess(false)
                    .target(to)
                    .channel(VerifyChannel.EMAIL)
                    .message("Email send fail,Please check the email provider & params.")
                    .build();
            log.error(result.getMessage(), e);
            return result;
        }
    }
}
