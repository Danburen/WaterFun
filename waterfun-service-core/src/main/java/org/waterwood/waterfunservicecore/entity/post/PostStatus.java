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

    private final short value;
    PostStatus(int value) {
        this.value = (short) value;
    }

    public static PostStatus fromCode(short code) {
        return switch (code) {
            case 0 -> DRAFT;
            case 1 -> PENDING;
            case 2 -> PUBLISHED;
            case 3 -> REJECTED;
            case 4 -> ARCHIVED;
            default -> throw new IllegalArgumentException("Invalid PostStatus code: " + code);
        };
    }

    public static final Map<PostStatus, Set<PostStatus>> ADMIN_TRANSITIONS = Map.of(
            DRAFT, Set.of(PENDING, PUBLISHED),
            PENDING, Set.of(PUBLISHED, DRAFT, REJECTED),
            PUBLISHED, Set.of(DRAFT, ARCHIVED),
            REJECTED, Set.of(DRAFT, PUBLISHED),
            ARCHIVED, Set.of(DRAFT, PUBLISHED)
    );
}
