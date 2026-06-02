package org.waterwood.waterfunservicecore.infrastructure.utils;

import org.waterwood.common.io.FileExtension;
import org.waterwood.utils.PathUtil;

import java.util.UUID;

public final class CosKeyPathGenerator {
    public static String ofUser(Long userUid, UUID uuid, FileExtension ext){
        return PathUtil.buildPath(
                userUid.toString(),
                PathUtil.getDataStampFilePath(uuid.toString().replace("-",""), ext)
        );
    }

    public static String of(UUID resourceUUID, FileExtension ext){
        return PathUtil.getDataStampFilePath(resourceUUID.toString().replace("-",""), ext);
    }

}
