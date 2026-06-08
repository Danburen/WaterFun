package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.BannerPosition;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BannerResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String linkUrl;
    private BannerPosition position;
    private Integer sortNo = 0;
    private Instant startAt;
    private Instant endAt;
    private CloudResPresignedUrlResp presignedUrl;
}
