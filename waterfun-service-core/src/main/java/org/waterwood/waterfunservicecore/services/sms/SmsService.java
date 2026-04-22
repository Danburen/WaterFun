package org.waterwood.waterfunservicecore.services.sms;

import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

import java.util.Map;

public interface SmsService {
    /**
     * Send sms code to a phone number segment template
     * @param phoneNumber target
     * @param templateCode template code
     * @param params params place to template
     * @return Optional String ofPending the response
     */
    CodeResult sendSms(String phoneNumber, String templateCode, Map<String, Object> params);
}
