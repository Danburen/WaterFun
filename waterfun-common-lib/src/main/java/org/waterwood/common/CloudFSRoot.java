package org.waterwood.common;


import lombok.Getter;

@Getter
public enum CloudFSRoot {
    UPLOADS("uploads"),
    SYSTEM("temp"),
    MODERATION("moderation"),
    USER("user");

    private final String key;
    CloudFSRoot(String key) {
        this.key = key;
    }

}
