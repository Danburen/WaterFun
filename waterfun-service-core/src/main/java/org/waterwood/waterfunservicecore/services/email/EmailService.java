package org.waterwood.waterfunservicecore.services.email;

import org.waterwood.api.enums.EmailTemplateType;

import java.util.Map;

public interface EmailService {
    /**
     * Send email with Html type content by {@link EmailTemplateType}
     * @param to send to whom
     * @param type type of email
     * @param data inject data to template context
     */
    EmailCodeResult sendHtmlEmail(String to, EmailTemplateType type, Map<String, Object> data);
    /**
     * Send email with Html type content
     * by choosing <b>base template</b> with <b>content template</b>
     * @param to send to whom
     * @param from form whom
     * @param subject subject of email
     * @param baseTemplate baseTemplate
     * @param contentTemplate content Template
     * @param data data to inject into context
     */
    EmailCodeResult sendHtmlEmail(String to, String from, String subject, String baseTemplate, String contentTemplate, Map<String, Object> data);

    /**
     * Send raw html email
     * @param to the target
     * @param from from target
     * @param subject subject of email
     * @param html raw html.
     * @return {@link EmailCodeResult}
     */
    EmailCodeResult sendHtmlEmail(String to, String from, String subject, String html);

    /**
     * Send simple text email
     * @param to target
     * @param from from target
     * @param subject subject of email
     * @param text text
     * @return {@link EmailCodeResult}
     */
    EmailCodeResult sendSimpleEmail(String to, String from, String subject, String text);
}
