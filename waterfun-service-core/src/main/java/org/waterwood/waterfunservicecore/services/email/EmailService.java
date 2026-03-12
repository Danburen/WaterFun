package org.waterwood.waterfunservicecore.services.email;

import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

import java.util.Map;

public interface EmailService {
    /**
     * Send email segment Html type content
     * by choosing <b>base template</b> segment <b>content template</b>
     * @param to send to whom
     * @param from form whom
     * @param subject subject of email
     * @param baseTemplate baseTemplate
     * @param contentTemplate content Template
     * @param data data to inject into context
     */
    CodeResult sendHtmlEmail(String to, String from, String subject, String baseTemplate, String contentTemplate, Map<String, Object> data);

    /**
     * Send raw html email
     * @param to the target
     * @param from from target
     * @param subject subject of email
     * @param html raw html.
     * @return the result of email send
     */
    CodeResult sendHtmlEmail(String to, String from, String subject, String html);

    /**
     * Send simple text email
     * @param to target
     * @param from from target
     * @param subject subject of email
     * @param text text
     * @return the result of email send
     */
    CodeResult sendSimpleEmail(String to, String from, String subject, String text);
}
