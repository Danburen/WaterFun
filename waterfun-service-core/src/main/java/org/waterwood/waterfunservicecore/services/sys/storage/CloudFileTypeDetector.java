package org.waterwood.waterfunservicecore.services.sys.storage;

import org.waterwood.common.io.SimpleCloudObject;

public interface CloudFileTypeDetector {
    /**
     * Detect a cloud file type by file's magic number
     *
     * @param fullCloudFilePathKey path key
     * @return MIME type of file and size, <b>application/octet-stream</b> if unknown
     */
    SimpleCloudObject detectByMagicNumber(String fullCloudFilePathKey);
}
