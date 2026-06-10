package org.waterwood.waterfunservice.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservice.api.response.post.TagResponse;
import org.waterwood.waterfunservice.infrastructure.mapper.TagMapper;
import org.waterwood.waterfunservice.service.post.TagService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/post/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final TagMapper tagMapper;

    @GetMapping("/hot")
    public ApiResponse<Page<TagResponse>> getHotTags(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(tagService.getHotTags(pageable)
                .map(tagMapper::toResponseDto)
        );
    }
    @GetMapping("/search/options")
    public ApiResponse<List<OptionVO<Long>>> searchTagOptions(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "10") @Positive @Max(20) int limit) {
        return ApiResponse.success(
                tagService.searchTags(keyword, limit).stream()
                        .map(t -> new OptionVO<>(t.getId(), t.getSlug(), t.getName(), false))
                        .toList()
        );
    }

    @GetMapping("/search")
    public ApiResponse<List<TagResponse>> searchTags(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "10") @Positive @Max(20) int limit) {
        return ApiResponse.success(
                tagService.searchTags(keyword, limit).stream().map(tagMapper::toResponseDto).toList()
        );
    }

    @GetMapping("/me")
    public ApiResponse<List<TagResponse>> getTags(){
        List<Tag> tagList = tagService.getSelfTags();
        return ApiResponse.success(
                tagList.stream().map(tagMapper::toResponseDto).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> getTag(@PathVariable Long id){
        Tag tag = tagService.getTag(id);
        return ApiResponse.success(tagMapper.toResponseDto(tag));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id){
        tagService.deleteTag(id);
        return ApiResponse.success();
    }
}
