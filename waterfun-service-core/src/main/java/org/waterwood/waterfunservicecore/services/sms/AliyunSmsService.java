package org.waterwood.waterfunservicecore.services.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.configuration.AliyunSmsConfig;

import static com.aliyun.teautil.Common.toJSONString;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class AliyunSmsService implements SmsService {
    private Client client;
    @Value("${aliyun.sms.sign-name}")
    private String signName;
    public AliyunSmsService() {
        try{
            client = AliyunSmsConfig.getClient();
        }catch (Exception e){
            log.error("Can't create Aliyun client instance{}", e.getMessage());
            client = null;
        }
    }

    @Override
    public CodeResult sendSms(String phoneNumber, String templateCode, Map<String, Object> params) {
        if(client == null){
            log.error("Fail send Sms code to {},cause:{}",phoneNumber,"Can't get client instance");
            return CodeResult.builder()
                    .sendSuccess(false)
                    .target(phoneNumber)
                    .channel(VerifyChannel.SMS)
                    .build();
        }
        SendSmsRequest sendSmsRequest = new SendSmsRequest()
                .setPhoneNumbers(phoneNumber)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam(JsonUtil.toJson(params));
        try {
            SendSmsResponse sendSmsResponse = client.sendSms(sendSmsRequest);
            String message = sendSmsResponse.getBody().getMessage();
            message = new String(message.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            return CodeResult.builder()
                    .sendSuccess(sendSmsResponse.getBody().getCode() != null && sendSmsResponse.getBody().getCode().equals("OK"))
                    .target(phoneNumber)
                    .channel(VerifyChannel.SMS)
                    .build();
        }catch (Exception e){
            throw new ServiceException("Fail send Sms code to " + phoneNumber + ",cause:" + e.getMessage());
        }
    }
}
