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
import org.waterwood.api.enums.PostStatus;
import org.waterwood.waterfunadminservice.api.request.DeletePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.AssignTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.CreatePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.PutPostReq;
import org.waterwood.waterfunadminservice.api.response.content.PostResponse;
import org.waterwood.waterfunadminservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunadminservice.service.content.PostService;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RequireRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.PostSpec;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/posts")
@RequireRole("ADMIN")
public class PostController {
    private final PostService postService;
    private final PostMapper postMapper;

    @GetMapping("/list")
    public ApiResponse<Page<PostResponse>> listPosts(@RequestParam(required = false) String title,
                                                     @RequestParam(required = false) PostStatus status,
                                                     @RequestParam(required = false) Integer categoryId,
                                                     @RequestParam(required = false) Long authorId,
                                                     @RequestParam(required = false) List<Integer> tagIds,
                                                     @RequestParam(required = false) String slug,
                                                     @PageableDefault() Pageable pageable) {
        Specification<Post> spec = PostSpec.of(title, status, categoryId, authorId, tagIds, slug);
        Page<Post> posts = postService.listPosts(spec, pageable);
        return ApiResponse.success(
                posts.map(postMapper::toPostResponseDto)
        );
    }

    @GetMapping("/id")
    public ApiResponse<PostResponse> getPostById(@RequestParam Long id) {
        return ApiResponse.success(
                postMapper.toPostResponseDto(postService.getPostById(id))
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
}
