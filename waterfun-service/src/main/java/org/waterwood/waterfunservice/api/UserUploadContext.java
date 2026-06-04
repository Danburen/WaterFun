package org.waterwood.waterfunservice.api;

import lombok.*;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadContext;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUploadContext<T extends Serializable> implements UploadContext<T, UserBizType> {
    private String resourceUuid;
    private T bizId;
    @Builder.Default
    private String cosKey = null;
    private UserBizType bizType;

    public BizUploadPayload toPayload() {
        BizUploadPayload payload = new BizUploadPayload();
        payload.setResourceUuid(this.getResourceUuid());
        payload.setCosKey(this.getCosKey());
        payload.setBizId(String.valueOf(this.getBizId()));
        payload.setBizType(this.getBizType().name());
        return payload;
    }

//    public static <T extends Serializable> UserUploadContext<T> fromPayload(BizUploadPayload payload, Class<T> targetType) {
//        return payload.toContext(UserBizType.class, targetType, UserUploadContext::new);
//    }
}
