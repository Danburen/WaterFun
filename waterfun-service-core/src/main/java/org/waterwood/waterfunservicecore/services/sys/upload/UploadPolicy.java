package org.waterwood.waterfunservicecore.services.sys.upload;

import org.waterwood.waterfunservicecore.api.BizType;

import java.util.List;

public interface UploadPolicy {
    BizType getBizType();
    String getBizId();
    List<String> getExts();
}
