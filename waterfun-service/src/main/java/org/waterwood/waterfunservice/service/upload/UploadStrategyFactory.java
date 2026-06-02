package org.waterwood.waterfunservice.service.upload;

import org.springframework.stereotype.Component;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UploadStrategyFactory {
    private final Map<BizType, UploadBizStrategy> strategies;
    public UploadStrategyFactory(List<UploadBizStrategy> strategies) {
        this.strategies = new HashMap<>();
        strategies.forEach( strategy -> {
            strategy.getTargetBizTypes().forEach( bizType -> {
                if (this.strategies.containsKey(bizType)) {
                    throw new IllegalStateException("Duplicate UploadBizStrategy for bizType: " + bizType);
                }
                this.strategies.put(bizType, strategy);
            });
        });
    }

    public UploadBizStrategy getStrategy(BizType bizType) {
        UploadBizStrategy strategy = this.strategies.get(bizType);
        if (strategy == null) {
            throw new IllegalStateException("No UploadBizStrategy for bizType: " + bizType);
        }
        return strategy;
    }

    public UploadBizStrategy getStrategy(BizUploadPayload payload) {
        try{
            UploadBizStrategy strategy;
            BizType type = BizType.valueOf(payload.getBizType().toUpperCase());
            strategy = this.strategies.get(type);
            return strategy;
        }  catch (Exception e) {
            throw new IllegalStateException("Invalid bizType in payload: " + payload.getBizType(), e);
        }
    }
}
