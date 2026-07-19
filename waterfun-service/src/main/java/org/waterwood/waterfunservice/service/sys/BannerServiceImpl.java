package org.waterwood.waterfunservice.service.sys;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.waterfunservice.api.response.BannerResponse;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.content.Banner;
import org.waterwood.waterfunservicecore.entity.content.VisibleStatus;
import org.waterwood.waterfunservicecore.infrastructure.persistence.BannerRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BannerServiceImpl implements BannerService {
    private final BannerRepository bannerRepository;
    private final CloudFileService cloudFileService;

    public BannerServiceImpl(BannerRepository bannerRepository, CloudFileService cloudFileService) {
        this.bannerRepository = bannerRepository;
        this.cloudFileService = cloudFileService;
    }

    @Override
    public List<BannerResponse> getAllActiveBanner() {
        List<Banner> banners = bannerRepository
                .findCurrentlyActive(
                        Instant.now(), VisibleStatus.SHOW, false
                );
        Map<Long, String> bannerIdResourceCosKeyMap =
                banners.stream().collect(
                        Collectors.toMap(
                                Banner::getId,
                                v-> v.getResource().getResourceKey()
                        )
                );
        Map<Long, CloudResPresignedUrlResp> bannerIdPresignedUrlMap =
                cloudFileService.batchGetReadPublicUrlCached(
                        CloudFSRoot.SYSTEM,
                        bannerIdResourceCosKeyMap,
                        TargetType.BANNER_IMAGE
                );
        return banners.stream().map(b -> {
            BannerResponse br = new BannerResponse();
            br.setId(b.getId());
            br.setPosition(b.getPosition());
            br.setTitle(b.getTitle());
            br.setSubtitle(b.getSubtitle());
            br.setLinkUrl(b.getLinkUrl());
            br.setSortNo(b.getSortNo());
            br.setPresignedUrl(bannerIdPresignedUrlMap.get(b.getId()));
            return br;
        }).toList();
    }
}
