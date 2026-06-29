package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeContentDTO implements NotificationCoverageContent {
    private List<Long> userUids;
    private String nativeUrl;
    private CloudResPresignedUrlResp postCoverage;

    @Override
    public void setCoveragePresignedUrl(CloudResPresignedUrlResp resp) {
        this.postCoverage = resp;
    }
}
