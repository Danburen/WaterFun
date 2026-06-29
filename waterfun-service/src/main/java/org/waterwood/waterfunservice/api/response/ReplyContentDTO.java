package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyContentDTO implements NotificationCoverageContent {
    private Long replierUid;
    private String replyContent;
    private String nativeUrl;
    private CloudResPresignedUrlResp postCoverage;

    @Override
    public void setCoveragePresignedUrl(CloudResPresignedUrlResp resp) {
        this.postCoverage = resp;
    }
}
