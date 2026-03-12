package org.waterwood.waterfunservicecore.services.sys.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.common.KeyConstants;
import org.waterwood.common.cache.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.HttpMethod;
import org.waterwood.waterfunservicecore.api.resp.PostPolicyResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.services.sys.CloudKeyBuilder;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TencentCosService implements CloudFileService {
    private final COSClient cosClient;
    @Value("${cloud.tencent.cos.bucket-name}")
    private String bucketName;
    @Value("${cloud.tencent.cos.default-expires-seconds}")
    private long defaultExpires;
    @Value("${cloud.tencent.cos.safety-marge-seconds}")
    private long safetyMarge;
    @Value("${cloud.tencent.cos.upload-expires-seconds}")
    private long uploadExpires;
    private final RedisHelperHolder redisHelper;
    @Value("${cloud.biz-prefix}")
    private String bizPrefix;
    @Override
    public void uploadFile(String key, InputStream stream, long size, String contentType) {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(size);
        meta.setContentType(contentType);
        PutObjectRequest putReq = new PutObjectRequest(bucketName, key,stream, meta);
        cosClient.putObject(putReq);
    }

    @Override
    public CloudResPresignedUrlResp getReadPublicUrlCached(String path, Serializable bizId, Duration dur, CloudResType resType) {
        String cacheKey = getCachedRedisKey(
                bizId,
                resType,
                CloudResOperationType.READ
        );
        String cached = redisHelper.getValue(cacheKey);
        String url;
        Date expiration;
        if(cached ==  null){
            expiration = Date.from(Instant.now().plus(dur));
            url = cosClient.generatePresignedUrl(
                    bucketName,
                    bizPrefix + "/" + path,
                    expiration,
                    HttpMethodName.GET).toString();
            redisHelper.set(cacheKey, url, dur.minusSeconds(safetyMarge));
        }else{
            url = cached;
            expiration = Date.from(Instant.now()
                    .plusSeconds(redisHelper.getExpire(cacheKey)));
        }
        return new CloudResPresignedUrlResp(
                url,
                expiration.toInstant()
        );
    }

    @Override
    public CloudResPresignedUrlResp getReadPublicUrlCached(String path, Serializable bizId, CloudResType resType) {
        return getReadPublicUrlCached(path, bizId, Duration.ofSeconds(defaultExpires), resType);
    }

    @Override
    public PostPolicyResp buildUploadPutPolicy(String path) {
        String keyPath = bizPrefix + "/" + KeyConstants.UPLOADS + "/" + path;
        Date expire = Date.from(Instant.now().plus(Duration.ofSeconds(uploadExpires)));
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName,  keyPath, HttpMethodName.PUT);
        request.setExpiration(expire);
        URL url = cosClient.generatePresignedUrl(request);
        return new PostPolicyResp(
                keyPath,
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

    @Override
    public void removeUploadsFile(String path) {
        removeFile(KeyConstants.UPLOADS + "/" + path);
    }

    @Override
    public List<CloudResPresignedUrlResp> batchGetReadPublicUrlCached(List<String> paths, List<String> bizIds, Duration dur, CloudResType cloudResType) {
        List<String> keys = IntStream.range(0, paths.size())
                .mapToObj(i -> RedisKeyBuilder.buildKey(
                        CloudKeyBuilder.fs(),
                        CloudResOperationType.READ.getKey(),
                        cloudResType.getLocalCase(),
                        bizIds.get(i)))
                .toList();
        List<String> cached = redisHelper.mget(keys);

        List<Integer> missIdx = new ArrayList<>();
        for (int i = 0; i < cached.size(); i++) {
            if (cached.get(i) == null) missIdx.add(i);
        }
        Date expiration = Date.from(Instant.now().plus(dur));
        Map<Integer, String> idxAndUrl = new HashMap<>();
        missIdx.forEach(idx -> {
            String url = cosClient.generatePresignedUrl(
                    bucketName,
                    bizPrefix + "/" + paths.get(idx),
                    expiration,
                    HttpMethodName.GET).toString();
            idxAndUrl.put(idx, url);
        });

        return List.of();
    }

    @Override
    public String getCachedRedisKey(Serializable bizId, CloudResType resType, CloudResOperationType operationType) {
        return RedisKeyBuilder.buildKey(
                CloudKeyBuilder.fs(),
                operationType.getKey(),
                resType.getLocalCase(),
                bizId.toString()
        );
    }

    @Override
    public List<CloudResPresignedUrlResp> batchGetReadPublicUrlCached(List<String> paths, List<String> bizIds, CloudResType cloudResType) {
        return batchGetReadPublicUrlCached(paths, bizIds, Duration.ofSeconds(defaultExpires), cloudResType);
    }
}
