package org.waterwood.waterfunservicecore.api;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum EmailTemplateLayout {
    SUBSCRIBE_BASE("subscribe_base"),
    VERIFY_BASE("verify_base"),;
    private final String templateKey;
    private final String defaultFrom = "WaterFun<noreply@mail.waterfun.top>";

    EmailTemplateLayout(String templateKey) {
        this.templateKey = templateKey;
    }
    private final Set<VerifyScene> scenes =
            Set.of(VerifyScene.LOGIN, VerifyScene.REGISTER, VerifyScene.SET_PASSWORD, VerifyScene.CHANGE_EMAIL, VerifyScene.RESET_PASSWORD);

    private static final Map<VerifyScene, EmailTemplateLayout> SCENE_MAP =
            Arrays.stream(values())
                    .flatMap(e -> e.scenes.stream().map(s -> Map.entry(s, e)))
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a));

    public static EmailTemplateLayout ofScene(VerifyScene scene) {
        return SCENE_MAP.getOrDefault(scene, VERIFY_BASE);
    }
}
