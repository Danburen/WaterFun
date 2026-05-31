package org.waterwood.waterfunservice.service.moderation;

import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ModerationCallbackStrategyFactory {
    private final Map<TargetType, ModerationCallbackStrategy> strategies;

    public ModerationCallbackStrategyFactory(List<ModerationCallbackStrategy> strategies) {
        this.strategies = new HashMap<>();
        strategies.forEach(strategy ->
                strategy.getTargetTypes().forEach(type -> {
                    ModerationCallbackStrategy existing = this.strategies.put(type, strategy);
                    if (existing != null) {
                        throw new IllegalStateException(
                                "Moderation type " + type + " is claims by more than one moderation strategy: "
                                        + existing.getClass().getSimpleName() + " vs "
                                        + strategy.getClass().getSimpleName()
                        );
                    }
                })
        );
    }

    public ModerationCallbackStrategy getStrategy(TargetType type) {
        ModerationCallbackStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new UnsupportedOperationException("Unregister moderation type: " + type);
        }
        return strategy;
    }
}
