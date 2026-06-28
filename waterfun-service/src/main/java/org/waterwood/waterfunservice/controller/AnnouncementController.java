package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.response.post.PostCardResp;
import org.waterwood.waterfunservice.service.post.PostService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<Page<PostCardResp>> listAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.listAnnouncements(PageRequest.of(page, size)));
    }
}
