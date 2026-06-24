package org.waterwood.waterfunservicecore.services.sys.storage;

import org.jetbrains.annotations.NotNull;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileProbeResult;
import org.waterwood.common.io.ResourceType;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CloudFileService {
    /**
     * Upload a file to cloud storage
     * @param key the key ofPending the file
     * @param stream file input stream {@link InputStream}
     * @param size file size
     * @param contentType file content type
     */
    @Deprecated
    void uploadFile(String key, InputStream stream, long size, String contentType);
    /**
     * Get file from cloud storage
     *
     * @param root root key of cloud storage, which act as the first level key for cloud storage
     * @param path    url path
     * @param bizId   business id, which act as cache key for cache store
     * @param resType resource type
     * @return direct url which can be used to access the file
     */
    CloudResPresignedUrlResp getReadUrlCached(CloudFSRoot root, String path, Serializable bizId, TargetType resType);

    /**
     * Build put policy with business key
     *
     * @param root    root key of cloud file service
     * @param path    path of the resource must contain, Usually generate by{@link CosKeyPathGenerator}
     * @param payload packaged biz target id
     * @return Presigned request response contains the url and key for file upload, and the key will be used for file access after upload success.
     */
    PresignedResp buildPutPolicyWithPayload(CloudFSRoot root, String path, BizUploadPayload payload);

    /**
     * Build put policy with business key with key root = {@link CloudFSRoot#UPLOADS}
     * also see {@link #buildPutPolicyWithPayload(CloudFSRoot, String, BizUploadPayload)}
     * @param path    path of the resource must contain, Usually generate by{@link CosKeyPathGenerator}
     * @param payload packaged biz target id
     * @return Presigned request response contains the url and key for file upload, and the key will be used for file access after upload success.
     */
    PresignedResp buildPutPolicyForUploads(String path, BizUploadPayload payload);

    /**
     * Batch build put policy with business key with key root = {@link CloudFSRoot#UPLOADS}
     *
     * @param paths    List paths of the resource must contain, Usually generate by{@link CosKeyPathGenerator}
     * @param payloads packaged biz target id
     * @return List of Presigned request response contains the url and key for file upload, and the key will be used for file access after upload success.
     */
    List<PresignedResp> batchBuildPutPolicyForUploads(List<String> paths, List<BizUploadPayload> payloads);

    /**
     * Remove file from cloud storage
     *
     * @param root root key
     * @param key  the key of Pending the file
     */
    void removeFile(CloudFSRoot root, String key);

    /**
     * Batch remove files
     * @param cloudFSRoot root key
     * @param keys the keys of Pending the file
     */
    void removeFiles(CloudFSRoot cloudFSRoot, List<String> keys);

    /**
     * Batch get file from cloud storage
     * <b>BizIds</b> must correspond with paths
     *
     * @param rootKey             cloud resource key root
     * @param bizIdNonRootPathMap bizId and no root path map.
     * @param cloudResType        resource type
     * @return map of Pending direct url which can be used to access the file,
     * null value in map means the file is not exist or failed to get url,
     * or paths is null, and the key of map is the bizId provided in parameter.
     */
    <ID extends Serializable> Map<ID, CloudResPresignedUrlResp> batchGetReadPublicUrlCached(CloudFSRoot rootKey, Map<ID, String> bizIdNonRootPathMap, TargetType cloudResType);

    String getCachedRedisKey(Serializable bizId, TargetType resType, CloudResOperationType operationType);
    List<String> batchGetCachedRedisKey(List<Serializable> bizIds, TargetType resType, CloudResOperationType operationType);

    /**
     * Detect cloud file type by file's magic number, and check if the file type is matched with asserted cloud file type.
     * <p>IF file type is not allow, this method will try to remove target file</p>
     * @param root          Cloud File storage key root
     * @param KeyPath       cloud file key path
     * @param allowResTypes asserted cloud file type.
     * @return simple object contains cloud file info, such as content type and size with key.
     * @throws BizException if the cloud file type is not matched.
     */
    FileProbeResult detectAndAssertCloudFile(CloudFSRoot root, String KeyPath, ResourceType... allowResTypes);

    /**
     * Copy file from origin path to target path, and remove the old file.
     * @param originalRoot original root key path
     * @param originPath original path
     * @param targetRoot target cloud root key
     * @param targetPath target Path
     */
    void copyFileAndRemoveOld(CloudFSRoot originalRoot, String originPath, CloudFSRoot targetRoot, String targetPath);

    /**
     * Clear file get cache
     * @param bizId business id
     * @param targetType resource type
     */
    void clearGetCache(Serializable bizId, TargetType targetType);

    /**
     * Parse String type biz payload
     * @param token upload token
     * @return String ID payload
     */
    BizUploadPayload parseToken(String token);


    /**
     * Set up a resource before upload callback or for uploading preparing
     * @param res resource, if null, a new resource will be created
     * @param uuidPlain no dash plain uuid string
     * @param path cos path without root
     * @param userUid user uid, null for system
     */
    void createAndSetUpUploadRes(Resource res, String uuidPlain, String path, Long userUid);
    /**
     * Create a new resource and set up a resource before upload callback or for uploading preparing.
     * @param uuidPlain no dash plain uuid string
     * @param path cos path without root
     * @param userUid user uid, null for system
     * @return created resource entity with id and resource key, but not persist to database, caller
     * need to save it.
     * @see CloudFileService#createAndSetUpUploadRes(Resource, String, String, Long)
     */
    Resource createAndSetUpUploadRes(String uuidPlain, String path, Long userUid);

    /**
     * Set up resource callback and validate the uploader whether is same as requesting upload policy before,
     * if is different throw
     *
     * @param res            resource entity must not be null
     * @param root           cloud file root key, used for detect and assert the resource
     * @param resourceStatus resource status to be asserted and detect. when is pass the check, it will set up to the resource
     * @param resourceType   available resource type
     * @throws org.waterwood.waterfunservicecore.exception.ForbiddenException if the uploader of resource is
     *                                                                        different from the user in upload policy payload, which means the resource is not belong to the upload
     *                                                                        request, and may be a malicious request.
     */
    void setAndValidResourceForCallback(Resource res, CloudFSRoot root, ResourceStatus resourceStatus, ResourceType... resourceType);
}
