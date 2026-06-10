package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.response.BannerResponse;
import org.waterwood.waterfunservice.service.sys.BannerService;
import org.waterwood.waterfunservicecore.entity.content.BannerPosition;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banners")
public class BannerController {
    private final BannerService bannerService;

    @GetMapping
    public ApiResponse<List<BannerResponse>> getAllBanner(){
        return ApiResponse.success(
                bannerService.getAllActiveBanner()
        );
    }

    @GetMapping("/by-position")
    public ApiResponse<List<BannerResponse>> getBannerByPosition(@RequestParam BannerPosition position){
        return ApiResponse.success(
                bannerService.getAllActiveBanner().stream()
                        .filter(banner -> banner.getPosition() == position)
                        .toList()
        );
    }
}
