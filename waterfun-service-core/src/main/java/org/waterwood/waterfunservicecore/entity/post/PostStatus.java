package org.waterwood.waterfunservicecore.entity.post;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
public enum PostStatus {
    DRAFT(0),
    PENDING(1),
    PUBLISHED(2),
    REJECTED(3),
    ARCHIVED(4);

    private final byte value;
    PostStatus(int value) {
        this.value = (byte) value;
    }

    public static PostStatus fromCode(Byte code) {
       for (PostStatus postStatus : PostStatus.values()) {
           if (postStatus.value == code) {
               return postStatus;
           }
       }
       throw new IllegalArgumentException("No PostStatus for code " + code);
    }

    public static final Map<PostStatus, Set<PostStatus>> ADMIN_TRANSITIONS = Map.of(
            DRAFT, Set.of(PENDING, PUBLISHED),
            PENDING, Set.of(PUBLISHED, DRAFT, REJECTED),
            PUBLISHED, Set.of(DRAFT, ARCHIVED),
            REJECTED, Set.of(DRAFT, PUBLISHED),
            ARCHIVED, Set.of(DRAFT, PUBLISHED)
    );
}
