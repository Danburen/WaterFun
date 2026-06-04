package org.waterwood.waterfunservicecore.services.sys.upload;

import org.waterwood.waterfunservicecore.api.BizType;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;

import java.io.Serializable;

public interface UploadContext<T extends Serializable, B extends BizType> {
    String getResourceUuid();
    T getBizId();
    String getCosKey();
    B getBizType();

    void setResourceUuid(String resourceUuid);
    void setBizId(T bizId);
    void setCosKey(String cosKey);
    void setBizType(B bizType);

    BizUploadPayload toPayload();
}
