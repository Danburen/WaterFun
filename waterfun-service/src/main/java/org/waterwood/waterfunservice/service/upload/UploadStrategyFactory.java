package org.waterwood.waterfunservice.service.upload;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservicecore.api.BizType;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadBizStrategy;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadStrategyProducer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Getter
public class UploadStrategyFactory implements UploadStrategyProducer<UserUploadPolicyReq> {
    private final Map<String, UploadBizStrategy<UserUploadPolicyReq>> strategies;
    public UploadStrategyFactory(List<UploadBizStrategy<UserUploadPolicyReq>> strategies) {
        this.strategies = new HashMap<>();
        strategies.forEach( strategy -> {
            strategy.getTargetBizTypeCodes().forEach(bizType -> {
                if (this.strategies.containsKey(bizType)) {
                    throw new IllegalStateException("Duplicate UploadBizStrategy for bizType: " + bizType);
                }
                this.strategies.put(bizType, strategy);
            });
        });
    }

    @Override
    public @NotNull Map<String, UploadBizStrategy<UserUploadPolicyReq>> getStrategies() {
        return Collections.unmodifiableMap(strategies);
    }
}
