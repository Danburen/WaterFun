package org.waterwood.waterfunadminservice.service.upload;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunadminservice.api.request.AdminUploadPolicyReq;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadBizStrategy;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadStrategyProducer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Getter
public class AdminUploadStrategyFactory implements UploadStrategyProducer<AdminUploadPolicyReq> {
    private final Map<String, UploadBizStrategy<AdminUploadPolicyReq>> strategies;
    public AdminUploadStrategyFactory(List<UploadBizStrategy<AdminUploadPolicyReq>> strategies) {
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
    public @NotNull Map<String, UploadBizStrategy<AdminUploadPolicyReq>> getStrategies() {
        return Collections.unmodifiableMap(strategies);
    }
}
