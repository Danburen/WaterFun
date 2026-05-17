package org.waterwood.waterfunservice.service.upload;

import org.springframework.stereotype.Component;
import org.waterwood.common.KeyConstants;
import org.waterwood.waterfunservice.api.BizType;
import org.waterwood.waterfunservicecore.utils.BizPayload;

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

    public UploadBizStrategy getStrategy(BizPayload payload) {
        UploadBizStrategy strategy = null;
        if(payload.getBiz().equals(KeyConstants.USER)){
            strategy = switch (payload.getType()) {
                case KeyConstants.AVATAR -> this.strategies.get(BizType.AVATAR);
                default -> throw new IllegalStateException("No UploadBizStrategy for payload: " + payload);
            };
        }
        if(strategy == null) {
            throw new IllegalStateException("No UploadBizStrategy for payload: " + payload);
        }
        return strategy;
    }
}
