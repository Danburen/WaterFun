package org.waterwood.waterfunservicecore.services.email;

import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.waterwood.api.enums.EmailTemplateType;

import java.util.Map;

public abstract class EmailServiceBase implements EmailService {
    private final SpringTemplateEngine templateEngine;

    protected EmailServiceBase(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    @Override
    public EmailCodeResult sendHtmlEmail(String to, EmailTemplateType type, Map<String, Object> data) {
        return sendHtmlEmail(to,type.getDefaultFrom(),type.getSubject(),"email_base",type.getTemplateKey(),data);
    }

    @Override
    public EmailCodeResult sendHtmlEmail(String to, String from, String subject, String baseTemplate, String contentTemplate, Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);
        String contentPart = templateEngine.process("email/" + contentTemplate, context);

        context.setVariable("content", contentPart);
        String emailContent = templateEngine.process("email/" + baseTemplate , context);
        return sendHtmlEmail(to, from, subject, emailContent);
    }

    @Override
    public abstract EmailCodeResult sendHtmlEmail(String to, String from, String subject, String html);

    @Override
    public abstract EmailCodeResult sendSimpleEmail(String to, String from, String subject, String text);
}
