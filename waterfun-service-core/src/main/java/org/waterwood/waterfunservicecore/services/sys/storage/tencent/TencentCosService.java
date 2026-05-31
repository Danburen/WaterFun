package org.waterwood.waterfunservicecore.services.sys.storage.tencent;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.KeyConstants;
import org.waterwood.common.io.FileProbeResult;
import org.waterwood.common.io.ResourceType;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.resource.SourceType;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.utils.PathUtil;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.io.FileTypeNotAllowException;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import org.waterwood.waterfunservicecore.api.HttpMethod;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.CloudKeyBuilder;
import org.waterwood.waterfunservicecore.services.sys.storage.*;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
            redisHelper.hSetMap(buildUploadRedisKey(payload.getResourceUuid()),
                    payload.toMap(),
                    Duration.ofSeconds(uploadTokenExpires)
            );
            return new PresignedResp(
                    path,
                    url.toString(),
                    HttpMethod.PUT,
                    payload.getResourceUuid()
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
                    buildUploadRedisKey(payload.getResourceUuid()),
                    payload.toMap(),
                    Duration.ofSeconds(uploadTokenExpires)
            );

            results.add(new PresignedResp(
                    path,
                    url.toString(),
                    HttpMethod.PUT,
                    payload.getResourceUuid()
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
    public FileProbeResult detectAndAssertCloudFile(CloudFSRoot root, String KeyPath, CloudFileType cloudFileType) {
        String suffix = PathUtil.getSuffix(KeyPath);
        String fullPath = buildCosKey(root, KeyPath);
        if(!cloudFileType.matchSuffix(suffix)){
            throw new FileTypeNotAllowException(suffix, cloudFileType.getAllowFileExtensions());
        }
        new FileProbeResult();
        FileProbeResult meta;
        meta = cloudFileTypeDetector.detectByMagicNumber(fullPath);
        ContentType contentType = ContentType.getByMimeType(meta.getMimeType());
//        log.info(contentType.getMimeType());
        if(! cloudFileType.matchContentType(contentType.getMimeType())){
            throw new FileTypeNotAllowException(contentType.getMimeType(), cloudFileType.name());
        }
        return meta;
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
        return BizUploadPayload.fromMap(
//                redisHelper.hGetAllAndDel(
//                        buildUploadRedisKey(token)
//                )
                redisHelper.hGetAll(
                        buildUploadRedisKey(token)
                )
        );
    }

    @Override
    public void CreateAndSetUpUploadRes(Resource res, String uuidPlain, String path, Long userUid) {
        if(res == null){
            res = new Resource();
        }
        res.setUuid(uuidPlain);
        res.setResourceKey(path);
        res.setUploaderId(userUid == null ? 0L : userUid);
        res.setSourceType(userUid == null ? SourceType.SYSTEM : SourceType.USER_UPLOADED);
    }

    @Override
    public Resource CreateAndSetUpUploadRes(String uuidPlain, String path, Long userUid) {
        Resource res = new Resource();
        res.setUuid(uuidPlain);
        res.setResourceKey(path);
        res.setUploaderId(userUid == null ? 0L : userUid);
        res.setSourceType(userUid == null ? SourceType.SYSTEM : SourceType.USER_UPLOADED);
        return res;
    }

    @Override
    public void setAndValidResourceForCallback(@NotNull Resource res, CloudFSRoot root, ResourceStatus resourceStatus, ResourceType resourceType) {
        if(res.getUploaderId() == null || ! res.getUploaderId().equals(UserCtxHolder.getUserUid())){
            throw new ForbiddenException();
        }
        FileProbeResult probeResult = detectAndAssertCloudFile(root, res.getResourceKey(), CloudFileType.IMAGE);
        res.setResourceType(resourceType);
        res.setSizeBytes(probeResult.getSize());
        res.setMimeType(probeResult.getMimeType());
        res.setFileMeta(probeResult.getMeta().toJson());
        res.setStatus(resourceStatus); // only bind when temporary saving or publishing.
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
     * Batch get presigned URLs with Redis cache with default expiration.
     *
     * @param <ID>                id type, e.g. Long, String, or business semantic id like "coverage-<uuid>"
     * @param rootKey             cloud resource key root
     * @param bizIdNonRootPathMap bizId and path map, where path is the key path under cloud root, e.g. "2024/06/01/xxx.jpg"
     * @param cloudResType        resource type for building cache key, e.g. "coverage", "avatar", etc.
     * @return map of bizId and presigned URL response, where null value means the file is not exist or failed to get url. The bizId is the key of map, which is provided in parameter, and the URL is the value of map.
     * e.g., if bizIdPathMap is {123L: "2024/06/01/xxx.jpg"}, the returned map will be {123L: CloudResPresignedUrlResp}, where CloudResPresignedUrlResp contains the presigned URL and its expiration time.
     * @see CloudResPresignedUrlResp
     */
    @Override
    public <ID extends Serializable> Map<ID, CloudResPresignedUrlResp> batchGetReadPublicUrlCached(CloudFSRoot rootKey, Map<ID, String> bizIdNonRootPathMap, TargetType cloudResType) {
        Map<ID, String> bizIdFullPathMap = bizIdNonRootPathMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> buildCosKey(rootKey, e.getValue())
                ));
        return batchGetReadPublicUrlCached(
                bizIdFullPathMap,
                Duration.ofSeconds(defaultExpires),
                cloudResType
        );
    }

    /**
     * Batch get presigned URLs with Redis cache.
     *
     * @param <ID>         id type, e.g. Long, String, or business semantic id like "coverage-<uuid>"
     * @param bizIdCosPathMap bizId and cos path map, where path is the key path under cloud root, e.g. "root/2024/06/01/xxx.jpg"
     * @param dur          presigned URL duration, which also used as cache duration for hitting cache. Cache will have a safety marge to avoid returning expired URL.
     * @param cloudResType   resource type for building cache key, e.g. "coverage", "avatar", etc.
     * @return map of bizId and presigned URL response, where null value means the file is not exist or failed to get url. The bizId is the key of map, which is provided in parameter, and the URL is the value of map.
     *          e.g., if bizIdPathMap is {123L: "2024/06/01/xxx.jpg"}, the returned map will be {123L: CloudResPresignedUrlResp}, where CloudResPresignedUrlResp contains the presigned URL and its expiration time.
     * @see CloudResPresignedUrlResp
     */
    private <ID extends Serializable> Map<ID, CloudResPresignedUrlResp> batchGetReadPublicUrlCached(Map<ID, String> bizIdCosPathMap, Duration dur, TargetType cloudResType) {
        if (bizIdCosPathMap == null || bizIdCosPathMap.isEmpty()) {
            return Collections.emptyMap();
        }
        // build redis key
        List<Map.Entry<ID, String>> entries = new ArrayList<>(bizIdCosPathMap.entrySet());
        List<String> keys = new ArrayList<>(entries.size());

        for (Map.Entry<ID, String> entry : entries) {
            ID bizId = entry.getKey();
            String redisKey = getCachedRedisKey(bizId, cloudResType, CloudResOperationType.READ);
            keys.add(redisKey);
        }
        // batch try hit cache
        List<String> cached = redisHelper.mget(keys);
        Map<ID, CloudResPresignedUrlResp> result = new HashMap<>(bizIdCosPathMap.size());
        List<Integer> missIndices = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            String cachedUrl = cached.get(i);
            if (cachedUrl != null) {
                ID bizId = entries.get(i).getKey();
                long expireSeconds = redisHelper.getExpire(keys.get(i));
                result.put(bizId, new CloudResPresignedUrlResp(
                        cachedUrl,
                        Instant.now().plusSeconds(expireSeconds)
                ));
            } else {
                missIndices.add(i);
            }
        }
        // process miss indices
        if (!missIndices.isEmpty()) {
            Date expiration = Date.from(Instant.now().plus(dur));
            Map<String, String> toCache = new HashMap<>();

            for (int idx : missIndices) {
                Map.Entry<ID, String> entry = entries.get(idx);
                ID bizId = entry.getKey();
                String fullPath = entry.getValue();

                if (fullPath != null) {
                    String url = cosClient.generatePresignedUrl(
                            bucketName, fullPath, expiration, HttpMethodName.GET
                    ).toString();

                    result.put(bizId, new CloudResPresignedUrlResp(url, expiration.toInstant()));
                    toCache.put(keys.get(idx), url);
                } else {
                    result.put(bizId, null);
                }
            }

            if (!toCache.isEmpty()) {
                redisHelper.mset(toCache, dur.minusSeconds(safetyMarge));
            }
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
