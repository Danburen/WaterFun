package org.waterwood.waterfunadminservice.api.response.content.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationTaskPayloadRes {
    private PayloadType type;
    private ModerationResourceRes singleResource;
    private List<ModerationResourceRes> resources;

    private String content;
    private AuditContentFormat contentFormat;
    private AuditPayload meta;

    public enum PayloadType {
        SINGLE_RESOURCE,
        RICH_TEXT
    }
}

