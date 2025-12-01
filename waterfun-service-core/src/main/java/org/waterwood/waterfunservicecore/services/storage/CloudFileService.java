package org.waterwood.waterfunservicecore.services.storage;

import org.waterwood.waterfunservicecore.api.PostPolicyDto;

import java.io.InputStream;
import java.time.Duration;

public interface CloudFileService {
    /**
     * Upload a file to cloud storage
     * @param key the key of the file
     * @param stream file input stream {@link InputStream}
     * @param size file size
     * @param contentType file content type
     */
    void uploadFile(String key, InputStream stream, long size, String contentType);

    /**
     * Get file from cloud storage
     * @param path url path
     * @param duration cache duration
     * @return direct url which can be used to access the file
     */
    String getFileUrlFromCloud(String path, Duration duration);

    /**
     * Build put policy for cloud storage
     * @param path url path
     * @param suffix file suffix
     * @return put policy
     */
    PostPolicyDto buildPutPolicy(String path, String suffix);

    /**
     * Remove file from cloud storage
     * @param key the key of the file
     */
    void removeFile(String key);
}
