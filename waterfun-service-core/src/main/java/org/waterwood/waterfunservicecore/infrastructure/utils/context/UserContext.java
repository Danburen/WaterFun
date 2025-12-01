package org.waterwood.waterfunservicecore.infrastructure.utils.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class UserContext {
    private final Long userId;
    private final Set<String> roles;
}
