package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservice.api.request.content.CreateTagRequest;
import org.waterwood.waterfunservice.api.request.content.UpdateTagRequest;
import org.waterwood.waterfunservice.api.response.post.TagResponse;
import org.waterwood.waterfunservice.infrastructure.mapper.TagMapper;
import org.waterwood.waterfunservice.service.post.TagService;

import java.util.List;

@RestController
@RequestMapping("/api/post/tag")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final TagMapper tagMapper;

    @PostMapping
    public ApiResponse<Void> addTag(@RequestBody @Valid CreateTagRequest request){
        tagService.createTag(tagMapper.toEntity(request));
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> getTags(){
        List<Tag> tagList = tagService.getTags();
        return ApiResponse.success(
                tagList.stream().map(tagMapper::toResponseDto).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> getTag(@PathVariable Long id){
        Tag tag = tagService.getTag(id);
        return ApiResponse.success(tagMapper.toResponseDto(tag));
    }

    @PutMapping
    public ApiResponse<Void> updateTag(@RequestBody @Valid UpdateTagRequest request){
        tagService.updateTag(tagMapper.toEntity(request));
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id){
        tagService.deleteTag(id);
        return ApiResponse.success();
    }
}
