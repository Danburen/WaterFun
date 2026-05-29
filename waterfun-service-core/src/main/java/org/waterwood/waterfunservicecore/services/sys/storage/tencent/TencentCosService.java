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
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.KeyConstants;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.common.io.SimpleCloudObject;
import org.waterwood.utils.PathUtil;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import org.waterwood.waterfunservicecore.api.HttpMethod;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.task.TargetType;
import org.waterwood.waterfunservicecore.services.sys.CloudKeyBuilder;
import org.waterwood.waterfunservicecore.services.sys.storage.*;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

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
    public CloudResPresignedUrlResp getReadUrlCached(CloudFSRoot root, String path, Serializable bizId, TargetType resType) {
        return getReadUrlCached(
                buildCosKey(root, path),
                bizId,
                Duration.ofSeconds(defaultExpires),
                resType
        );
    }

    @Override
    public PresignedResp buildPutPolicyWithPayload(CloudFSRoot root, String path, BizUploadPayload payload) {
        String keyPath = buildCosKey(root, path);
        Date expire = Date.from(Instant.now().plus(Duration.ofSeconds(uploadExpires)));
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName,  keyPath, HttpMethodName.PUT);
        request.setExpiration(expire);

        COSClient client = uploadCosClientProvider.getIfAvailable();
        if (client != null) {
            URL url = client.generatePresignedUrl(request);
            // Token
            redisHelper.hSetMap(buildUploadRedisKey(payload.getUploadId()),
                    payload.toMap(),
                    Duration.ofSeconds(uploadTokenExpires)
            );
            return new PresignedResp(
                    path,
                    url.toString(),
                    HttpMethod.PUT,
                    payload.getUploadId()
            );
        }else{
            throw new BizException(BaseResponseCode.COS_UPLOAD_CLIENT_NOT_CONFIGURED);
        }
    }

    @Override
    public PresignedResp buildPutPolicyForUploads(String path, BizUploadPayload payload) {
        return buildPutPolicyWithPayload(CloudFSRoot.UPLOADS, path, payload);
    }

    @Override
    public List<PresignedResp> batchBuildPutPolicyForUploads(List<String> paths, List<BizUploadPayload> payloads) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }

        COSClient client = uploadCosClientProvider.getIfAvailable();
        if (client == null) {
            throw new BizException(BaseResponseCode.COS_UPLOAD_CLIENT_NOT_CONFIGURED);
        }

        Date expire = Date.from(Instant.now().plus(Duration.ofSeconds(uploadExpires)));
        List<PresignedResp> results = new ArrayList<>(paths.size());

        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            BizUploadPayload payload = payloads.get(i);
            String keyPath = buildCosKey(CloudFSRoot.UPLOADS, path);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, keyPath, HttpMethodName.PUT);
            request.setExpiration(expire);

            URL url = client.generatePresignedUrl(request);

            // Token
            redisHelper.hSetMap(
                    buildUploadRedisKey(payload.getUploadId()),
                    payload.toMap(),
                    Duration.ofSeconds(uploadTokenExpires)
            );

            results.add(new PresignedResp(
                    path,
                    url.toString(),
                    HttpMethod.PUT,
                    payload.getUploadId()
            ));
        }

        return results;
    }

    @Override
    public void removeFile(CloudFSRoot root, String key) {
        cosClient.deleteObject(bucketName,buildCosKey(root, key));
    }

    @Override
    public String getCachedRedisKey(Serializable bizId, TargetType resType, CloudResOperationType operationType) {
        return RedisKeyBuilder.buildKey(
                CloudKeyBuilder.fs(),
                operationType.getKey(),
                resType.toLowerCase(),
                bizId.toString()
        );
    }

    @Override
    public <ID extends Serializable> Map<ID, CloudResPresignedUrlResp> batchGetReadPublicUrlCached(CloudFSRoot rootKey, List<String> paths, List<ID> bizIds, TargetType cloudResType) {
        return batchGetReadPublicUrlCached(
                paths.stream().map(path -> buildCosKey(rootKey, path)).toList(),
                bizIds,
                Duration.ofSeconds(defaultExpires),
                cloudResType
        );
    }

    @Override
    public SimpleCloudObject detectAndAssertCloudFile(String KeyPath, CloudFileType cloudFileType) {
        String suffix = PathUtil.getSuffix(KeyPath);
        String fullPath = buildCosKey(CloudFSRoot.UPLOADS, KeyPath);
        if(!cloudFileType.matchSuffix(suffix)){
            throw new BizException(BaseResponseCode.FILE_TYPE_NOT_ALLOW, suffix, cloudFileType.getAllowFileExtensions());
        }
        SimpleCloudObject object = new SimpleCloudObject();
        object.setKey(KeyPath);
        object.setFileMeta(cloudFileTypeDetector.detectByMagicNumber(fullPath));
        ContentType contentType = ContentType.getByMimeType(object.getFileMeta().getMimeType());
//        log.info(contentType.getMimeType());
        if(! cloudFileType.matchContentType(contentType.getMimeType())){
            throw new BizException(BaseResponseCode.FILE_TYPE_NOT_ALLOW, contentType.getMimeType(), cloudFileType.name());
        }
        return object;
    }

    @Override
    public void copyFileAndRemoveOld(CloudFSRoot originalRoot, String originPath, CloudFSRoot targetRoot, String targetPath) {
        String originFullPath = buildCosKey(originalRoot, originPath);
        String targetFullPath = buildCosKey(targetRoot, targetPath);
        cosClient.copyObject(bucketName, originFullPath, bucketName, targetFullPath);
        removeFile(targetRoot, originFullPath);
    }

    @Override
    public void clearGetCache(Serializable bizId, TargetType targetType) {
        String cacheKey = getCachedRedisKey(
                bizId,
                targetType,
                CloudResOperationType.READ
        );
        redisHelper.del(cacheKey);
    }

    @Override
    public BizUploadPayload parseToken(String token) {
        BizUploadPayload payload = BizUploadPayload.fromMap(
                redisHelper.hGetAllAndDel(
                        buildUploadRedisKey(token)
                )
        );
        return payload;
    }

    private CloudResPresignedUrlResp getReadUrlCached(String fullPath, Serializable bizId, Duration dur, TargetType resType) {
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
                    fullPath,
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

    /**
     * Batch get presigned URLs with Redis cache.
     *
     * @param <ID> id type, e.g. Long, String, or business semantic id like "coverage-<uuid>"
     */
    private <ID extends Serializable> Map<ID, CloudResPresignedUrlResp> batchGetReadPublicUrlCached(List<String> fullPaths, List<ID> bizIds, Duration dur, TargetType cloudResType) {
        if (fullPaths == null || fullPaths.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = IntStream.range(0, fullPaths.size())
                .filter(i -> bizIds.get(i) != null)
                .mapToObj(i -> getCachedRedisKey(bizIds.get(i), cloudResType, CloudResOperationType.READ))
                .toList();

        List<String> cached = redisHelper.mget(keys);
        Map<ID, CloudResPresignedUrlResp> result = new HashMap<>(fullPaths.size());
        List<Integer> missIdx = new ArrayList<>();

        for (int i = 0; i < cached.size(); i++) {
            ID bizId = bizIds.get(i);
            if (cached.get(i) != null) {
                result.put(bizId, new CloudResPresignedUrlResp(
                        cached.get(i),
                        Date.from(Instant.now().plusSeconds(redisHelper.getExpire(keys.get(i)))).toInstant()
                ));

            } else {
                missIdx.add(i);
            }
        }

        if(! missIdx.isEmpty()) {
            Date expiration = Date.from(Instant.now().plus(dur));
            Map<String, String> toCache = new HashMap<>();
            for(int idx : missIdx) {
                ID bizId = bizIds.get(idx);
                if(fullPaths.get(idx) != null){
                    String url = cosClient.generatePresignedUrl(
                            bucketName, fullPaths.get(idx), expiration, HttpMethodName.GET
                    ).toString();
                    result.put(bizId, new CloudResPresignedUrlResp(url, expiration.toInstant()));
                    toCache.put(keys.get(idx), url);
                }else{
                    result.put(bizId, null);
                }
            }

            redisHelper.mset(toCache, dur);
        }

        return result;
    }

    private String buildUploadRedisKey(String token){
        return RedisKeyBuilder.buildKey(KeyConstants.UPLOADS, KeyConstants.TOKEN, token);
    }

    private String buildCosKey(CloudFSRoot root, String... keys){
        return PathUtil.buildPath(bizPrefix, root.getKey(), keys);
    }
}
