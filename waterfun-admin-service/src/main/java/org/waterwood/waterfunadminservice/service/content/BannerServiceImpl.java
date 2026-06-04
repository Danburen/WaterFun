package org.waterwood.waterfunadminservice.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.common.io.FileExtension;
import org.waterwood.common.io.ResourceType;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunadminservice.api.AdminUploadContext;
import org.waterwood.waterfunadminservice.api.request.AdminUploadPolicyReq;
import org.waterwood.waterfunadminservice.api.request.content.CreateBannerRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutBannerRequest;
import org.waterwood.waterfunadminservice.api.response.content.BannerResponse;
import org.waterwood.waterfunadminservice.infrastructure.mapper.BannerMapper;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.Banner;
import org.waterwood.waterfunservicecore.entity.BannerPosition;
import org.waterwood.waterfunservicecore.entity.VisibleStatus;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.ResourceUnavailableException;
import org.waterwood.waterfunservicecore.exception.notfound.BannerNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.ResourceReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.BannerRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.BizUploadPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.infrastructure.validation.UploadValidator;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.infrastructure.utils.CosKeyPathGenerator;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {
    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final CloudFileService cloudFileService;
    private final ResourceRepository resourceRepository;

    @Override
    public Page<Banner> list(Specification<Banner> spec, Pageable pageable) {
        return bannerRepository.findAll(spec, pageable);
    }

    @Override
    public Banner getById(Long id) {
        return bannerRepository.findById(id).orElseThrow(
                BannerNotFoundException::new
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
        Resource imageRes = resourceRepository.findByUuidAndStatus(req.getImageUuid(), ResourceStatus.ORPHAN)
                .orElseThrow(() -> new ResourceReferenceInvalidException(req.getImageUuid()));
        imageRes.setStatus(ResourceStatus.ACTIVE);
        banner.setResource(imageRes);
//        banner.setResourceKey(req.getPutCallback().getKey());
        resourceRepository.save(imageRes);
        bannerRepository.save(banner);
    }

    @Transactional
    @Override
    public void update(Long id, PutBannerRequest req) {
        Banner banner = getById(id);
        String newResUuid = req.getImageUuid();
        Resource oldRes = banner.getResource();

        if(!newResUuid.equals(oldRes.getUuid())){
            oldRes.setStatus(ResourceStatus.ORPHAN);
            Resource newRes = resourceRepository.findByUuidAndStatus(newResUuid, ResourceStatus.ORPHAN)
                    .orElseThrow(() -> new ResourceReferenceInvalidException(newResUuid));
            banner.setResource(newRes);
            newRes.setStatus(ResourceStatus.ACTIVE);
            resourceRepository.save(newRes);
        }
        bannerMapper.partialUpdate(req, banner);
        banner.setUpdatedAt(Instant.now());
        bannerRepository.save(banner);
    }

    @Override
    public BannerResponse getBanner(Long id) {
        Banner banner = bannerRepository.findById(id).orElseThrow(
                BannerNotFoundException::new
        );
        BannerResponse resp = bannerMapper.toResponse(banner);
        if(banner.getResource() == null) throw new ResourceReferenceInvalidException();
        if(banner.getResource().getStatus() != ResourceStatus.ACTIVE)
            throw new ResourceUnavailableException(banner.getResource().getUuid());
        resp.setCoverageUrl(
                cloudFileService.getReadUrlCached(
                        CloudFSRoot.UPLOADS,
                        banner.getResource().getResourceKey(),
                        "banner-coverage-" + banner.getId(),
                        TargetType.POST_COVERAGE_IMAGE
                )
        );
        return resp;
    }

    @Override
    public List<PresignedResp> handleBannerUpload(AdminUploadPolicyReq request) {
//        Long bannerId = Long.valueOf(request.getBizId());
//        Banner banner = bannerRepository.findById(bannerId).orElseThrow(
//                BannerNotFoundException::new
//        );
        FileExtension ext = UploadValidator.validateSingleFileUpload(request, TargetType.BANNER_IMAGE);

        UUID resourceUUID = UUID.randomUUID();
        BizUploadPayload payload = BizUploadPayload.of(
                UserCtxHolder.getUserUid(),
                request.getBizType().getCode(),
                resourceUUID
        );

        String cosPath = CosKeyPathGenerator.of(resourceUUID, ext);
        resourceRepository.save(
                cloudFileService.createAndSetUpUploadRes(
                        StringUtil.noDashUUIDString(resourceUUID),
                        CosKeyPathGenerator.of(resourceUUID, ext),
                        UserCtxHolder.getUserUid()
                )
        );
        return List.of(cloudFileService.buildPutPolicyWithPayload(
                CloudFSRoot.USER,
                cosPath,
                payload)
        );
    }

    @Override
    public void handleBannerUploadCallback(CloudPutCallbackReq request, AdminUploadContext<Long> ctx) {
//        Long bannerId = Long.parseLong(payload.getBizId());
//        Banner banner = bannerRepository.findById(bannerId).orElseThrow(
//                BannerNotFoundException::new
//        );
        Long uploaderUid = UserCtxHolder.getUserUid();
        if(! uploaderUid.equals(ctx.getBizId())) throw new ForbiddenException();
        String resourceUuid = ctx.getResourceUuid();
        Resource res = resourceRepository.findByUuidAndStatus(
                ctx.getResourceUuid(),
                ResourceStatus.UPLOAD_PENDING
        ).orElseThrow(() -> new ResourceReferenceInvalidException(resourceUuid));
        cloudFileService.setAndValidResourceForCallback(
                res,
                CloudFSRoot.UPLOADS,
                ResourceStatus.ORPHAN,
                ResourceType.IMAGE
        );
        resourceRepository.save(res);
    }
}
