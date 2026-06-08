package org.waterwood.waterfunadminservice.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunadminservice.service.content.AdminBizType;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadContext;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUploadContext <T extends Serializable> implements UploadContext<T, AdminBizType> {
    private String resourceUuid;
    private T bizId;
    @Builder.Default
    private String cosKey = null;
    private AdminBizType bizType;

    public BizUploadPayload toPayload() {
        BizUploadPayload payload = new BizUploadPayload();
        payload.setResourceUuid(this.getResourceUuid());
        payload.setCosKey(this.getCosKey());
        payload.setBizId(String.valueOf(this.getBizId()));
        payload.setBizType(this.getBizType().name());
        return payload;
    }
}
