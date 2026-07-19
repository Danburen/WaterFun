package org.waterwood.waterfunadminservice.controller.content;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.request.content.CreateBannerRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutBannerRequest;
import org.waterwood.waterfunadminservice.api.response.content.BannerResponse;
import org.waterwood.waterfunadminservice.infrastructure.mapper.BannerMapper;
import org.waterwood.waterfunadminservice.service.content.BannerService;
import org.waterwood.waterfunservicecore.entity.content.Banner;
import org.waterwood.waterfunservicecore.entity.content.BannerPosition;
import org.waterwood.waterfunservicecore.entity.content.VisibleStatus;
import org.waterwood.waterfunservicecore.entity.spec.BannerSpec;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/content/banner")
@RequiredArgsConstructor
public class BannerController {
    private final BannerService bannerService;
    private final BannerMapper bannerMapper;
    private final CloudFileService cloudFileService;

    @GetMapping("/list")
    public ApiResponse<Page<BannerResponse>> list(@RequestParam(required = false) String title,
                                                  @RequestParam(required = false) String subtitle,
                                                  @RequestParam(required = false) BannerPosition position,
                                                  @RequestParam(required = false) VisibleStatus status,
                                                  @RequestParam(required = false) Instant startAt,
                                                  @RequestParam(required = false) Instant endAt,
                                                  @RequestParam(required = false) Boolean isDeleted,
                                                  @PageableDefault Pageable pageable) {
        // frontend sends 1-based page, Spring Data Pageable is 0-based
        pageable = PageRequest.of(Math.max(0, pageable.getPageNumber() - 1), pageable.getPageSize(), pageable.getSort());
        Specification<Banner> spec = BannerSpec.of(title, subtitle, position, status, startAt, endAt, isDeleted);
        return ApiResponse.success(bannerService.list(spec, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<BannerResponse> get(@PathVariable Long id) {
        return ApiResponse.success(bannerService.getBanner(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody @Valid CreateBannerRequest req) {
        bannerService.createCallback(req);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid PutBannerRequest req) {
        bannerService.update(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ApiResponse.success();
    }
}
