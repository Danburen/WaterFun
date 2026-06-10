package org.waterwood.waterfunadminservice.api.response.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.content.Banner;
import org.waterwood.waterfunservicecore.entity.content.BannerPosition;
import org.waterwood.waterfunservicecore.entity.content.VisibleStatus;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link Banner}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BannerResponse implements Serializable {
    private Long id;
    private CloudResPresignedUrlResp coverageUrl;
    private String title;
    private String subtitle;
    private String linkUrl;
    private BannerPosition position;
    private Integer sortNo;
    private VisibleStatus status;
    private Instant startAt;
    private Instant endAt;
    private Instant createdAt;
    private Instant updatedAt;
}

