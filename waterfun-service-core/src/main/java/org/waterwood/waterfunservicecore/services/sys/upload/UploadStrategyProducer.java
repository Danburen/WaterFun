package org.waterwood.waterfunservicecore.services.sys.upload;

import org.jetbrains.annotations.NotNull;
import org.waterwood.waterfunservicecore.api.BizType;
import org.waterwood.waterfunservicecore.exception.BizTypeNotAllowException;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;

import java.util.Arrays;
import java.util.Map;


public interface UploadStrategyProducer<T extends UploadPolicy> {
    /**
     * Get a strategy by bizType code. If not found, throw BizTypeNotAllowException with available codes.
     * @param bizType target biz type
     * @throws BizTypeNotAllowException if no strategy found for the given bizType
     * @return UploadBizStrategy
     */
    default UploadBizStrategy<T> getStrategy(BizType bizType) {
        return doGetStrategy(bizType.getCode());
    };

    /**
     * Get a strategy by bizType code. If not found, throw BizTypeNotAllowException with available codes.
     *
     * @param payload cloud file upload payload, which contains bizType code
     * @throws BizTypeNotAllowException if no strategy found for the given bizType
     * @return UploadBizStrategy
     */
    default UploadBizStrategy<T> getStrategy(BizUploadPayload payload){
        return doGetStrategy(payload.getBizType());
    };

    private UploadBizStrategy<T> doGetStrategy(String code) {
        UploadBizStrategy<T> strategy = getStrategies().get(code.toLowerCase());
        if (strategy == null) {
            throw new BizTypeNotAllowException(code, getAvailableCodes());
        }
        return strategy;
    }

    default String getAvailableCodes() {
        return  this.getStrategies().keySet().toString();
    }

    @NotNull Map<String, UploadBizStrategy<T>> getStrategies();
}
