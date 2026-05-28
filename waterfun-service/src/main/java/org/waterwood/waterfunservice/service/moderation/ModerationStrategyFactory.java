package org.waterwood.waterfunservice.service.moderation;

import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.entity.audit.task.TargetType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ModerationStrategyFactory {
    private final Map<TargetType, ModerationStrategy> strategies;

    public ModerationStrategyFactory(List<ModerationStrategy> strategies) {
        this.strategies = new HashMap<>();
        strategies.forEach(strategy ->
                strategy.getTargetTypes().forEach(type -> {
                    ModerationStrategy existing = this.strategies.put(type, strategy);
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

    public ModerationStrategy getStrategy(TargetType type) {
        ModerationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new UnsupportedOperationException("Unregister moderation type: " + type);
        }
        return strategy;
    }
}
