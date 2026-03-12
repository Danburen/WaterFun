package org.waterwood.waterfunservicecore.services.email;

import jakarta.mail.internet.MimeMessage;
import org.aspectj.apache.bcel.classfile.Code;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

@Service
public class SpringEmailService extends EmailServiceBase {
    @Value("${spring.mail.username}")
    private String username;

    private final JavaMailSender mailSender;
    protected SpringEmailService(SpringTemplateEngine templateEngine, JavaMailSender mailSender) {
        super(templateEngine);
        this.mailSender = mailSender;
    }

    @Override
    public CodeResult sendHtmlEmail(String to, String from, String subject, String html) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        try{
            helper.setFrom(username);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        }catch (Exception e){
            return CodeResult.builder()
                    .target(to)
                    .sendSuccess(false)
                    .build();
        }
        return CodeResult.success(to, VerifyChannel.EMAIL);
    }

    @Override
    public CodeResult sendSimpleEmail(String to, String from, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(username);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        return CodeResult.success(to, VerifyChannel.EMAIL);
    }
}
