package org.waterwood.waterfunservice.api;

import lombok.*;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.utils.BizUploadPayload;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadContext<T extends Serializable>{
    private String uploadId;
    private T bizId;
    @Builder.Default
    private String cosKey = null;
    private BizType bizType;

    public BizUploadPayload toPayload() {
        BizUploadPayload payload = new BizUploadPayload();
        payload.setUploadId(this.getUploadId());
        payload.setCosKey(this.getCosKey());
        payload.setBizId(String.valueOf(this.getBizId()));
        return payload;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> UploadContext<T> fromPayload(BizUploadPayload payload,  Class<T> targetType) {
        UploadContext<T> context = new UploadContext<T>();
        context.setUploadId(payload.getUploadId());
        context.setCosKey(payload.getCosKey());
        if (StringUtil.isBlank(payload.getBizId())) {
            context.setBizId(null);
        }else{
            try{
                if (targetType == Long.class || targetType == long.class) {
                    context.setBizId((T) Long.valueOf(payload.getBizId()));
                } else if (targetType == Integer.class || targetType == int.class) {
                    context.setBizId((T) Integer.valueOf(payload.getBizId()));
                } else if (targetType == String.class) {
                    context.setBizId((T) payload.getBizId());
                } else {
                    throw new IllegalArgumentException("Unsupported bizId type: " + targetType);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid biz id: " + payload.getBizId());
            }
        }
        try {
            context.setBizType(BizType.valueOf(payload.getBizType().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid biz type: " + payload.getBizType().toUpperCase());
        }
        return context;
    }
}
