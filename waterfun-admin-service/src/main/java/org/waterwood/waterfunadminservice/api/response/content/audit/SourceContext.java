package org.waterwood.waterfunadminservice.api.response.content.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourceContext {
    private PostBrief sourcePostBrief;
}
