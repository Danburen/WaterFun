package org.waterwood.waterfunservice.infrastructure.dto;

import lombok.Value;

import java.util.Base64;

@Value
public class RootCommentCursor {
    Boolean isPined;
    Integer likeCount;
    Long id;
    public static RootCommentCursor first() {
        return new RootCommentCursor(false, Integer.MAX_VALUE, Long.MAX_VALUE);
    }

    public String encode() {
        String raw = (isPined ? 1 : 0) + ":" + likeCount + ":" + id;
        return Base64.getUrlEncoder().encodeToString(raw.getBytes());
    }

    public static RootCommentCursor decode(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor));
        String[] parts = raw.split(":");
        return new RootCommentCursor(
                "1".equals(parts[0]),
                Integer.parseInt(parts[1]),
                Long.parseLong(parts[2])
        );
    }
}
