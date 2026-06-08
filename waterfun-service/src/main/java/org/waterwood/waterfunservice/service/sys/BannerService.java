package org.waterwood.waterfunservice.service.sys;

import org.waterwood.waterfunservice.api.response.BannerResponse;

import java.util.List;

public interface BannerService {
    /**
     * Get all active banner
     * @return list of banner response
     */
    List<BannerResponse> getAllActiveBanner();
}
