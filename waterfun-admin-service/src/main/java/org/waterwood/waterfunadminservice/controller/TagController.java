package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.content.CreateTagRequest;
import org.waterwood.waterfunadminservice.api.request.content.DeleteTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateTagReq;
import org.waterwood.waterfunadminservice.api.response.content.TagResponse;
import org.waterwood.waterfunadminservice.infrastructure.mapper.TagMapper;
import org.waterwood.waterfunadminservice.service.content.TagService;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.TagSpec;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tags")
public class TagController {
    private final TagService tagService;
    private final TagMapper tagMapper;

    @PostMapping
    public ApiResponse<Void> createTag(@RequestBody @Valid CreateTagRequest req) {
        tagService.createTag(req);
        return ApiResponse.success();
    }

    @GetMapping("/list")
    public ApiResponse<Page<TagResponse>> listTags(@RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String slug,
                                                   @RequestParam(required = false) Long creatorId,
                                                   @RequestParam(required = false) Instant createdStart,
                                                   @RequestParam(required = false) Instant createdEnd,
                                                   @PageableDefault Pageable pageable) {
        Specification<Tag> spec = TagSpec.of(name, slug, creatorId, createdStart, createdEnd);
        Page<Tag> tags = tagService.list(spec, pageable);
        return ApiResponse.success(tags.map(tagMapper::toResponseDto));
    }

    @DeleteMapping
    public ApiResponse<BatchResult> deleteTags(@RequestBody @Valid DeleteTagsRequest req){
        BatchResult result = tagService.deleteTags(req);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> getTag(@PathVariable Integer id){
        Tag tag = tagService.getTag(id);
        return ApiResponse.success(tagMapper.toResponseDto(tag));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Integer id) {
        tagService.deleteTagById(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> putTag(@PathVariable Integer id, @RequestBody @Valid UpdateTagReq req){
        tagService.updateTag(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionVO<Integer>>> getOptions() {
        return ApiResponse.success(
                tagService.getOptions()
        );
    }
}
