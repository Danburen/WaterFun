package org.waterwood.common;


import lombok.Getter;

@Getter
public enum CloudFSRoot {
    UPLOADS("uploads"),
    SYSTEM("sys"),
    MODERATION("moderation"),
    USER("user");

    private final String key;
    CloudFSRoot(String key) {
        this.key = key;
    }

}
