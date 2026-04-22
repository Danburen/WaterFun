package org.waterwood.waterfunadminservice.api.response.content.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationTaskPayloadRes {
    private PayloadType type;
    private ModerationResourceRes singleResource;
    private List<ModerationResourceRes> resources;
    private String renderedContent;

    public enum PayloadType {
        SINGLE_RESOURCE,
        RICH_TEXT,
        PLAIN_TEXT
    }
}

