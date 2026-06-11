package org.waterwood.waterfunadminservice.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunadminservice.api.response.AuditTaskRes;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.PostAuditPayload;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditResponse <T extends AuditPayload> extends AuditTaskRes {
    private T payload;
    private List<ModerationResourceRes> resources;
}
