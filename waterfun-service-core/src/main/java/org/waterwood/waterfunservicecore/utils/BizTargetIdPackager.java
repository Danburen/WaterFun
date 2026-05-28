package org.waterwood.waterfunservicecore.utils;

import org.jetbrains.annotations.NotNull;
import org.waterwood.common.KeyConstants;

import java.util.UUID;

public final class BizTargetIdPackager {
    public static BizUploadPayload ofUser(Long userUid, @NotNull String domain, UUID resourceUUID){
        return new BizUploadPayload(
                KeyConstants.USER,
                String.valueOf(userUid),
                domain,
                null,
                resourceUUID.toString().replace("-", "")
        );
    }

    public static BizUploadPayload ofPost(Long bizId, String bizType, UUID uploadId) {
        return new BizUploadPayload(
                KeyConstants.POST,
                String.valueOf(bizId),
                bizType,
                null,
                uploadId.toString().replace("-", "")
        );
    }
}
