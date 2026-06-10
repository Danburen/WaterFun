package org.waterwood.waterfunservicecore.entity;

import lombok.Getter;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;

@Getter
public enum BanPermission {
    BAN_LOGIN("ban:login", "Login Ban", "禁止登录", BanReasonType.UNSPECIFIED),
    BAN_POST("ban:post", "Post Ban", "禁止发帖", BanReasonType.VIOLATION_OF_GUIDELINES),
    BAN_COMMENT("ban:comment", "Comment Ban", "禁止评论", BanReasonType.INAPPROPRIATE_CONTENT),
    BAN_UPLOAD("ban:upload", "Upload Ban", "禁止上传", BanReasonType.OTHER),
    BAN_CHAT("ban:chat", "Chat Ban", "禁止聊天", BanReasonType.TROLLING),
    BAN_CREATE("ban:create", "Content Create Ban", "禁止创建内容", BanReasonType.VIOLATION_OF_GUIDELINES);

    private final String code;
    private final String name;
    private final String description;
    private final BanReasonType defaultBanReason;

    BanPermission(String code, String name, String description, BanReasonType defaultBanReason) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.defaultBanReason = defaultBanReason;
    }
}
