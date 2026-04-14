package org.waterwood.waterfunservicecore.services.sys.storage.tencent;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.CloudStorageRootKey;
import org.waterwood.common.KeyConstants;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.common.io.SimpleCloudObject;
import org.waterwood.utils.PathUtil;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.HttpMethod;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.services.sys.CloudKeyBuilder;
import org.waterwood.waterfunservicecore.services.sys.storage.*;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
public class TencentCosService implements CloudFileService {

    private final COSClient cosClient;
    private final ObjectProvider<COSClient> uploadCosClientProvider;
    private final CloudFileTypeDetector cloudFileTypeDetector;
    @Value("${cloud.tencent.cos.bucket-name}")
    private String bucketName;
    @Value("${cloud.tencent.cos.default-expires-seconds:3600}")
    private long defaultExpires;
    @Value("${cloud.tencent.cos.safety-marge-seconds:300}")
    private long safetyMarge;
    @Value("${cloud.tencent.cos.upload-expires-seconds:300}")
    private long uploadExpires;
    @Value("${cloud.tencent.cos.upload-token-expires-seconds:300}")
    private long uploadTokenExpires;
    private final RedisHelperHolder redisHelper;
    @Value("${cloud.biz-prefix}")
    private String bizPrefix;

    @Autowired
    public TencentCosService(
            COSClient cosClient,
            @Qualifier("uploadCosClient") ObjectProvider<COSClient> uploadCosClientProvider,
            RedisHelperHolder redisHelper,
            CloudFileTypeDetector cloudFileTypeDetector) {
        this.cosClient = cosClient;
        this.uploadCosClientProvider = uploadCosClientProvider;
        this.redisHelper = redisHelper;
        this.cloudFileTypeDetector = cloudFileTypeDetector;
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
                    buildCosKey(CloudStorageRootKey.UPLOADS, path),
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
    public PresignedResp buildPutPolicyWithBiz(CloudStorageRootKey keyRoot, String path, Serializable bizIdx) {
        String keyPath = buildCosKey(keyRoot, path);
        Date expire = Date.from(Instant.now().plus(Duration.ofSeconds(uploadExpires)));
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName,  keyPath, HttpMethodName.PUT);
        request.setExpiration(expire);

        COSClient client = uploadCosClientProvider.getIfAvailable();
        if (client != null) {
            URL url = client.generatePresignedUrl(request);
            String uuidKey = UUID.randomUUID().toString().replace("-", "");
            redisHelper.set(RedisKeyBuilder.buildKey(KeyConstants.UPLOADS, KeyConstants.TOKEN, uuidKey),
                    bizIdx.toString(),
                    Duration.ofSeconds(uploadTokenExpires)
            );
            return new PresignedResp(
                    keyPath,
                    url.toString(),
                    HttpMethod.PUT,
                    uuidKey
            );
        }else{
            throw new BizException(BaseResponseCode.COS_UPLOAD_CLIENT_NOT_CONFIGURED);
        }
    }

    @Override
    public void removeFile(String key) {
        if(StringUtil.isNotBlank(key)){
            cosClient.deleteObject(bucketName, PathUtil.buildPath(bizPrefix, key));
        }
    }

    @Override
    public void removeFile(CloudStorageRootKey rootKey, String path) {
        removeFile(buildCosKey(rootKey, path));
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
                    buildCosKey(CloudStorageRootKey.UPLOADS, paths.get(idx)),
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

    @Override
    public String buildCosKey(CloudStorageRootKey root, String... keys){
        return PathUtil.buildPath(bizPrefix, root.getKey(), keys);
    }

    @Override
    public SimpleCloudObject detectAndAssertCloudFile(String fullKeyPath, CloudFileType cloudFileType) {
        String suffix = PathUtil.getSuffix(fullKeyPath);
        if(!cloudFileType.matchSuffix(suffix)){
            throw new BizException(BaseResponseCode.FILE_TYPE_NOT_ALLOW, suffix, cloudFileType.getAllowFileExtensions());
        }
        SimpleCloudObject object = cloudFileTypeDetector.detectByMagicNumber(fullKeyPath);
        ContentType contentType = ContentType.getByMimeType(object.getType());
//        log.info(contentType.getMimeType());
        if(! cloudFileType.matchContentType(contentType.getMimeType())){
            throw new BizException(BaseResponseCode.FILE_TYPE_NOT_ALLOW, contentType.getMimeType(), cloudFileType.name());
        }
        return object;
    }
}
