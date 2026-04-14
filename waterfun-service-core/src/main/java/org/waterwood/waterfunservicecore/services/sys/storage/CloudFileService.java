package org.waterwood.waterfunservicecore.services.sys.storage;

import org.waterwood.common.CloudStorageRootKey;
import org.waterwood.common.io.SimpleCloudObject;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;

import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;

public interface CloudFileService {
    /**
     * Upload a file to cloud storage
     * @param key the key of the file
     * @param stream file input stream {@link InputStream}
     * @param size file size
     * @param contentType file content type
     */
    @Deprecated
    void uploadFile(String key, InputStream stream, long size, String contentType);

    /**
     * Get file from cloud storage, will save to cache.
     *
     * @param path  url path
     * @param bizId
     * @param dur   cache duration
     * @param tp    resource type
     * @return direct url which can be used to access the file
     */
    CloudResPresignedUrlResp getReadPublicUrlCached(String path, Serializable bizId, Duration dur, CloudResType tp);

    /**
     * Get file from cloud storage
     *
     * @param path    url path
     * @param bizId   business id, which act as cache key for cache store
     * @param resType resource type
     * @return direct url which can be used to access the file
     */
    CloudResPresignedUrlResp getReadPublicUrlCached(String path, Serializable bizId, CloudResType resType);


    PresignedResp buildPutPolicyWithBiz(CloudStorageRootKey keyRoot, String path, Serializable bizIdx);

    /**
     * Remove file from cloud storage
     * @param key the key of the file
     */
    void removeFile(String key);

    void removeFile(CloudStorageRootKey rootKey, String path);

    /**
     * Batch get file from cloud storage
     * @param paths url paths
     * @param bizIds business ids
     * @param cloudResType resource type
     * @return list of direct url which can be used to access the file
     */
    List<CloudResPresignedUrlResp> batchGetReadPublicUrlCached(List<String> paths, List<String> bizIds, CloudResType cloudResType);
    /**
     * Batch get file from cloud storage
     * @param paths url paths
     * @param bizIds business ids
     * @param cloudResType resource type
     * @return list of direct url which can be used to access the file
     */
    List<CloudResPresignedUrlResp> batchGetReadPublicUrlCached(List<String> paths, List<String> bizIds, Duration dur,CloudResType cloudResType);

    String getCachedRedisKey(Serializable bizId, CloudResType resType, CloudResOperationType operationType);

    String buildCosKey(CloudStorageRootKey root, String... keys);

    /**
     * Detect cloud file type by file's magic number, and check if the file type is matched with asserted cloud file type.
     * @param fullKeyPath   cloud file key full path
     * @param cloudFileType asserted cloud file type.
     * @return simple object contains cloud file info, such as content type and size with key.
     * @throws org.waterwood.common.exceptions.BizException if the cloud file type is not matched.
     */
    SimpleCloudObject detectAndAssertCloudFile(String fullKeyPath, CloudFileType cloudFileType);
}
