package org.waterwood.waterfunservicecore.utils;

import org.jetbrains.annotations.NotNull;
import org.waterwood.common.KeyConstants;

import java.util.UUID;

public final class BizTargetIdPackager {
    public static BizUploadPayload ofUser(Long userUid, @NotNull String domain, UUID resourceUUID){
        return new BizUploadPayload(
                resourceUUID.toString().replace("-", ""), // uploadId
                KeyConstants.USER,                            // biz
                String.valueOf(userUid),                      // bizId
                domain,                                       // type (domain)
                null                                          // cosKey
        );
    }

    public static BizUploadPayload ofPost(Long bizId, String bizType, UUID uploadId) {
        return new BizUploadPayload(
                uploadId.toString().replace("-", ""), // uploadId
                KeyConstants.POST,                         // biz
                String.valueOf(bizId),                     // bizId
                bizType,                                    // type
                null                                        // cosKey
        );
    }
}
