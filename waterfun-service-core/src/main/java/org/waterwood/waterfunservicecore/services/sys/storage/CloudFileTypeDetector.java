package org.waterwood.waterfunservicecore.services.sys.storage;

import org.waterwood.common.io.FileMeta;

public interface CloudFileTypeDetector {
    /**
     * Detect a cloud file type by file's magic number
     *
     * @param fullCloudFilePathKey path key
     * @return MIME type ofPending file and size, <b>application/octet-stream</b> if unknown
     */
    FileMeta detectByMagicNumber(String fullCloudFilePathKey);
}
