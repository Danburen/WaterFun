package org.waterwood.waterfunservicecore.services.storage;

import cn.hutool.core.date.DateUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.HttpMethod;
import org.waterwood.waterfunservicecore.api.PostPolicyDto;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class TencentCosService implements CloudFileService {
    private final COSClient cosClient;
    @Value("${tencent.cos.bucket-name}")
    private String bucketName;
    public TencentCosService(COSClient cosClient){
        this.cosClient = cosClient;
    }

    @Override
    public void uploadFile(String key, InputStream stream, long size, String contentType) {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(size);
        meta.setContentType(contentType);
        PutObjectRequest putReq = new PutObjectRequest(bucketName, key,stream, meta);
        cosClient.putObject(putReq);
    }

    @Override
    public String getFileUrlFromCloud(String path, Duration duration) {
        Date expiration = Date.from(Instant.now().plus(duration));
        URL url = cosClient.generatePresignedUrl(
                bucketName,
                path,
                expiration,
                HttpMethodName.GET);
        return url.toString();
    }

    @Override
    public PostPolicyDto buildPutPolicy(String path ,String suffix) {
        String uuid = UUID.randomUUID().toString();
        String key = "uploads/" + path + "/" + DateUtil.today().replace("-", "/") + "/" + uuid + "." + suffix;
        Date expire = Date.from(Instant.now().plus(Duration.ofSeconds(5 * 60)));
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethodName.PUT);
        request.setExpiration(expire);
        URL url = cosClient.generatePresignedUrl(request);
        return new PostPolicyDto(
                key,
                url.toString(),
                HttpMethod.PUT
        );
    }

    @Override
    public void removeFile(String key) {
        if(StringUtil.isNotBlank(key)){
            cosClient.deleteObject(bucketName, key);
        }
    }
}
