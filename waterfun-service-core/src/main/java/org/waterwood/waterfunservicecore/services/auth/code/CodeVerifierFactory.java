package org.waterwood.waterfunservicecore.services.auth.code;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.api.VerifyChannel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CodeVerifierFactory {
    private final Map<VerifyChannel, CodeVerifier> verifierMap;
    public CodeVerifierFactory(List<CodeVerifier> verifiers) {
        verifierMap = verifiers.stream()
                .filter(v -> v.channel() != null)
                .collect(Collectors.toUnmodifiableMap(CodeVerifier::channel, Function.identity()));
    }

    public CodeVerifier of(VerifyChannel channel) {
        return channel == null ? null : verifierMap.get(channel);
    }
}
