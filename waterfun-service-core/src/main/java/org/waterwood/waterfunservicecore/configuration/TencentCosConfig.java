package org.waterwood.waterfunservicecore.configuration;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.tencent.cloud.CosStsClient;
import com.tencent.cloud.Response;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.waterwood.common.exceptions.ServiceException;

import java.util.TreeMap;

@Configuration
public class TencentCosConfig {
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    // TODO: Tencent cloud manufacture env change config strategy.
    @Bean
    @Primary
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

    @Bean("uploadCosClient")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public COSClient uploadCosClient(
            @Value("${cloud.tencent.cos.bucket-name}") String bucketName,
            @Value("${cloud.tencent.cos.upload-expires-seconds}") int uploadExpires,
            @Value("${cloud.biz-prefix}") String bizPrefix
    ) {
        String secretId = System.getenv("TENCENTCLOUD_SECRET_ID");
        String secretKey = System.getenv("TENCENTCLOUD_SECRET_KEY");
        String region = getEnvOrDefault();
        if (secretId == null || secretKey == null) {
            throw new ServiceException(secretId == null ? "Couldn't find tencent cloud secret id"
                    : "Couldn't find tencent cloud secret key");
        }

        try {
            TreeMap<String, Object> config = new TreeMap<>();
            config.put("secretId", secretId);
            config.put("secretKey", secretKey);
            config.put("durationSeconds", uploadExpires);
            config.put("bucket", bucketName);
            config.put("region", region);
            config.put("policy", buildUploadPolicy(bucketName, region, bizPrefix));

            Response response = CosStsClient.getCredential(config);
            if (response == null || response.credentials == null) {
                throw new ServiceException("Failed to get Tencent COS STS credential");
            }

            COSCredentials sessionCred = new BasicSessionCredentials(
                    response.credentials.tmpSecretId,
                    response.credentials.tmpSecretKey,
                    response.credentials.sessionToken
            );
            return new COSClient(sessionCred, new ClientConfig(new Region(region)));
        } catch (Exception e) {
            throw new ServiceException("Failed to create upload COS client by STS: " + e.getMessage());
        }
    }

    private String buildUploadPolicy(String bucketName, String region, String bizPrefix) {
        String appId = parseAppId(bucketName);
        String safePrefix = bizPrefix == null ? "" : bizPrefix;
        String resource = String.format("qcs::cos:%s:uid/%s:%s/%s/*", region, appId, bucketName, safePrefix);
        return "{" +
                "\"version\":\"2.0\"," +
                "\"statement\":[{" +
                "\"action\":[\"name/cos:PutObject\",\"name/cos:InitiateMultipartUpload\",\"name/cos:UploadPart\",\"name/cos:CompleteMultipartUpload\"]," +
                "\"effect\":\"allow\"," +
                "\"resource\":[\"" + resource + "\"]," +
                "\"condition\":{" +
                "\"numeric_less_than_equal\":{\"cos:content-length\":\"" + MAX_UPLOAD_BYTES + "\"}," +
                "\"string_like\":{\"cos:content-type\":\"image/*\"}" +
                "}" +
                "}]}";
    }

    private String parseAppId(String bucketName) {
        int split = bucketName.lastIndexOf('-');
        if (split < 0 || split == bucketName.length() - 1) {
            throw new ServiceException("cloud.tencent.cos.bucket-name should contain appId suffix");
        }
        return bucketName.substring(split + 1);
    }

    private String getEnvOrDefault() {
        String value = System.getenv("TENCENTCLOUD_REGION");
        return value == null || value.isBlank() ? "ap-shanghai" : value;
    }
}
