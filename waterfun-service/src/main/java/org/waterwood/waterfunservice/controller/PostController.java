package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.ContentPreviewReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservice.api.response.post.*;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.entity.spec.PostSpec;
import org.waterwood.waterfunservice.service.post.PostService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
public class PostController {
    private final PostService postService;

    /***
     * Get All the posts by page and optional params;
     * @param categoryId category id
     * @param tagIds tag ids belong to the post
     * @return  page ofPending posts
     */
    @GetMapping("/list")
    @RateLimit(key = "listPublicPosts")
    public ApiResponse<Page<PostCardResp>> listPublicPosts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false)List<Integer> tagIds,
            @RequestParam(defaultValue = "1")  @Positive int page,
            @RequestParam(defaultValue = "10") int size
            ){

        Specification<Post> spec = PostSpec.ofPublic(categoryId, tagIds, null);
        Pageable pageable = PageRequest
                .of(Math.max(page - 1, 0), Math.min(size, 20))
                .withSort(Sort.Direction.DESC, "publishedAt", "createdAt");
        return ApiResponse.success(postService.listCardPosts(spec, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostDetailResp> getPostDetail(@PathVariable Long id){
        return ApiResponse.success(postService.getPostDetail(id));
    }

    @GetMapping("/me/{id}")
    public ApiResponse<PostAuthorDetailResp> getMyPostDetail(@PathVariable Long id){
        return ApiResponse.success(postService.getSelfPostDetail(id));
    }
    @GetMapping("/me/list")
    public ApiResponse<Page<PostAuthorCardResp>> listMyPost(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) PostVisibility visibility,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false)List<Integer> tagIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Specification<Post> spec = PostSpec.ofSelf(status,visibility, categoryId, tagIds);
        return ApiResponse.success(postService.listAuthorCardPosts(spec, Pageable.ofSize(size).withPage(page - 1)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id){
        postService.deletePost(id);
        return ApiResponse.success();
    }

    @PostMapping("/draft")
    public ApiResponse<Long> draftNewPost(){
        return ApiResponse.success(postService.draftNew());
    }

    @GetMapping("/{id}/edit")
    public ApiResponse<PostDraftResp> getEditPostDraft(@PathVariable Long id){
        return ApiResponse.success(postService.getEditPostDraft(id));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publishPost(@PathVariable Long id, @Valid @RequestBody PostSaveReq req){
        postService.publish(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/content/preview")
    public ApiResponse<String> previewContent(@PathVariable Long id,@Valid  @RequestBody ContentPreviewReq req){
        return ApiResponse.success(postService.contentPreview(id, req.getContent()));
    }

    @PostMapping("/{id}/temp-save")
    public ApiResponse<Void> tempSavePost(@PathVariable Long id, @Valid @RequestBody PostSaveReq req){
        postService.save(id, req);
        return ApiResponse.success();
    }

    @RateLimit(key = "post.like", permits = 20)
    @PostMapping("/{id}/like")
    public ApiResponse<Void> like(@PathVariable Long id){
        postService.like(id);
        return ApiResponse.success();
    }

    @RateLimit(key = "post.collection", permits = 20)
    @PostMapping("/{id}/collection")
    public ApiResponse<Void> collection(@PathVariable Long id) {
        postService.collection(id);
        return ApiResponse.success();
    }

}
