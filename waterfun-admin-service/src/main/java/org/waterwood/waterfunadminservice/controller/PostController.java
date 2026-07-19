package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.DeletePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.AssignTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.CreatePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutPostReq;
import org.waterwood.waterfunadminservice.api.response.content.PostResponse;
import org.waterwood.waterfunadminservice.service.content.PostService;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.spec.PostSpec;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/posts")
public class PostController {
    private final PostService postService;

    @GetMapping("/list")
    public ApiResponse<Page<PostResponse>> listPosts(@RequestParam(required = false) String title,
                                                     @RequestParam(required = false) PostStatus status,
                                                     @RequestParam(required = false) Integer categoryId,
                                                     @RequestParam(required = false) Long authorId,
                                                     @RequestParam(required = false) List<Integer> tagIds,
                                                     @RequestParam(required = false) String slug,
                                                     @PageableDefault(sort = {"isPinned", "type"}, direction = Sort.Direction.DESC) Pageable pageable) {
        // frontend sends 1-based page, Spring Data Pageable is 0-based
        pageable = PageRequest.of(Math.max(0, pageable.getPageNumber() - 1), pageable.getPageSize(), pageable.getSort());
        Specification<Post> spec = PostSpec.of(title, status, categoryId, authorId, tagIds, slug);
        return ApiResponse.success(postService.listPosts(spec, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id) {
        return ApiResponse.success(
                postService.getPostDetailResponse(id)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePostById(@PathVariable Long id) {
        postService.deletePostById(id);
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<BatchResult> deletePosts(@RequestBody DeletePostRequest req) {
        return ApiResponse.success(
                postService.deletePosts(req)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> putPost(@PathVariable Long id,@RequestBody @Valid PutPostReq req) {
        postService.update(id, req);
        return ApiResponse.success();
    }

    @PostMapping
    public ApiResponse<Void> createPost(@RequestBody @Valid CreatePostRequest req){
        postService.createPost(req);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/tags")
    public ApiResponse<BatchResult> assignTags(@PathVariable Long id,@RequestBody AssignTagsRequest req){
        return ApiResponse.success(
                postService.assignTags(id, req)
        );
    }

    @PutMapping("/{id}/tags")
    public ApiResponse<BatchResult> replacePostTags(@PathVariable Long id, @RequestBody AssignTagsRequest req){
        return ApiResponse.success(
                postService.replaceTags(id, req)
        );
    }

    @DeleteMapping("/{id}/tags")
    public ApiResponse<BatchResult> deleteTags(@PathVariable Long id, @RequestBody AssignTagsRequest req){
        return ApiResponse.success(
                postService.deletePostTags(id, req)
        );
    }

    @PostMapping("/content/preview")
    public ApiResponse<String> previewContent(@RequestBody String content) {
        return ApiResponse.success(postService.previewContent(content));
    }
}
