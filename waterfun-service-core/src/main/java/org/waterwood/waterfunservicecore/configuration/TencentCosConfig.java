package org.waterwood.waterfunservicecore.configuration;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.waterwood.common.exceptions.ServiceException;

@Configuration
public class TencentCosConfig {
    // TODO: Tencent cloud manufacture env change config strategy.
    @Bean
    public COSClient cosClient(){
        String secretId = System.getenv("TENCENTCLOUD_SECRET_ID");
        String secretKey = System.getenv("TENCENTCLOUD_SECRET_KEY");
        if(secretId == null || secretKey == null){
            throw new ServiceException(secretId == null ? "Couldn't find tencent cloud secret id"
                    : "Couldn't find tencent cloud secret key");
        }
        COSCredentials cred = new BasicCOSCredentials(secretId,
                secretKey);
        Region region= new Region("ap-shanghai");
        ClientConfig clientConfig = new ClientConfig(region);
        return new COSClient(cred, clientConfig);
    }
}
