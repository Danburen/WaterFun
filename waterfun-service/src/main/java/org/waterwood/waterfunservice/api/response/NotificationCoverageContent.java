package org.waterwood.waterfunservice.api.response;

import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;

public interface NotificationCoverageContent extends NotificationContent {
    void setCoveragePresignedUrl(CloudResPresignedUrlResp resp);
}
