package org.waterwood.waterfunservicecore.api;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum EmailTemplateFragment {
    VERIFY_CODE("verify_code","email.subject.verify"),;
    private final String templateKey;
    private final String subject;
    EmailTemplateFragment(String templateKey, String subject) {
        this.templateKey = templateKey;
        this.subject = subject;
    }

    // Use switch if needed. as we have only one scene
    private final Set<VerifyScene> scenes =
            Set.of(VerifyScene.LOGIN, VerifyScene.REGISTER, VerifyScene.SET_PASSWORD, VerifyScene.CHANGE_EMAIL, VerifyScene.RESET_PASSWORD);

    private static final Map<VerifyScene, EmailTemplateFragment> SCENE_MAP =
            Arrays.stream(values())
                    .flatMap(e -> e.scenes.stream().map(s -> Map.entry(s, e)))
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a));

    public static EmailTemplateFragment ofScene(VerifyScene scene) {
        return SCENE_MAP.getOrDefault(scene, VERIFY_CODE);
    }
}