package org.waterwood.waterfunservicecore.services.email;

import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

import java.util.Map;

public abstract class EmailServiceBase implements EmailService {
    private final SpringTemplateEngine templateEngine;

    protected EmailServiceBase(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public CodeResult sendHtmlEmail(String to, String from,
                                    String subject,
                                    String baseTemplate,
                                    String contentTemplate,
                                    Map<String, Object> data) {
        Context ctx = new Context();
        ctx.setVariable("fragment", "fragment/" + contentTemplate);
        ctx.setVariables(data);
        String html = templateEngine.process("layout/" + baseTemplate , ctx);
        return sendHtmlEmail(to, from, subject, html);
    }

    @Override
    public abstract CodeResult sendHtmlEmail(String to, String from, String subject, String html);

    @Override
    public abstract CodeResult sendSimpleEmail(String to, String from, String subject, String text);
}
