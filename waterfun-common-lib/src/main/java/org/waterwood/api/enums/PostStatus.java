package org.waterwood.api.enums;

import java.util.Map;
import java.util.Set;

public enum PostStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    REJECTED,
    ARCHIVED;

    public static final Map<PostStatus, Set<PostStatus>> ADMIN_TRANSITIONS = Map.of(
            DRAFT, Set.of(PENDING, PUBLISHED),
            PENDING, Set.of(PUBLISHED, DRAFT, REJECTED),
            PUBLISHED, Set.of(DRAFT, ARCHIVED),
            REJECTED, Set.of(DRAFT, PUBLISHED),
            ARCHIVED, Set.of(DRAFT, PUBLISHED)
    );
}
