package org.waterwood.waterfunadminservice.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileExtension;
import org.waterwood.waterfunadminservice.api.request.content.CreateBannerRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutBannerRequest;
import org.waterwood.waterfunadminservice.api.response.content.BannerResponse;
import org.waterwood.waterfunadminservice.infrastructure.mapper.BannerMapper;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.Banner;
import org.waterwood.waterfunservicecore.entity.BannerPosition;
import org.waterwood.waterfunservicecore.entity.VisibleStatus;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.BannerRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {
    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final CloudFileService cloudFileService;

    @Override
    public Page<Banner> list(Specification<Banner> spec, Pageable pageable) {
        return bannerRepository.findAll(spec, pageable);
    }

    @Override
    public Banner getById(Long id) {
        return bannerRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Banner ID: " + id)
        );
    }

    @Override
    public void createCallback(CreateBannerRequest req) {
        Banner banner = bannerMapper.toEntity(req);
        if (banner.getPosition() == null) {
            banner.setPosition(BannerPosition.HOME);
        }
        if (banner.getSortNo() == null) {
            banner.setSortNo(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(VisibleStatus.SHOW);
        }
        Instant now = Instant.now();
        banner.setCreatedAt(now);
        banner.setUpdatedAt(now);
        banner.setResourceKey(req.getPutCallback().getKey());
        bannerRepository.save(banner);
    }

    @Override
    public void update(Long id, PutBannerRequest req) {
        Banner banner = getById(id);
        bannerMapper.partialUpdate(req, banner);
        banner.setUpdatedAt(Instant.now());

        bannerRepository.save(banner);
    }

    @Override
    public PresignedResp getCoverageUploadPolicy(String suffix) {
        UUID resourceUUID = UUID.randomUUID();
        return cloudFileService.buildPutPolicyWithPayload(
                CloudFSRoot.SYSTEM,
                CosKeyPathGenerator.of(resourceUUID, FileExtension.fromExt(suffix)),
                null
        );
    }

    @Override
    public BannerResponse getBanner(Long id) {
        Banner banner = getById(id);
        BannerResponse res = bannerMapper.toResponse(banner);
        res.setCoverageUrl(
                cloudFileService.getReadUrlCached(
                        CloudFSRoot.UPLOADS,
                        banner.getResourceKey(),
                        "banner-coverage-" + banner.getId(),
                        TargetType.POST_COVERAGE_IMAGE
                )
        );
        return res;
    }
}
