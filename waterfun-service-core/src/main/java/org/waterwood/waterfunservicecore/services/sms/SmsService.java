package org.waterwood.waterfunservicecore.services.sms;

import org.waterwood.waterfunservicecore.api.resp.auth.SmsCodeResult;

import java.util.Map;

public interface SmsService {
    /**
     * Send sms code to a phone number with template
     * @param phoneNumber target
     * @param templateCode template code
     * @param params params place to template
     * @return Optional String of the response
     */
    SmsCodeResult sendSms(String phoneNumber, String templateCode, Map<String, Object> params);
}
