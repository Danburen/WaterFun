package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.request.PutUserPostReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservice.api.response.post.PostAuthorCardResp;
import org.waterwood.waterfunservice.api.response.post.PostAuthorDetailResp;
import org.waterwood.waterfunservice.api.response.post.PostCardResp;
import org.waterwood.waterfunservice.api.response.post.PostDetailResp;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservice.infrastructure.mapper.PostMapper;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.PostSpec;
import org.waterwood.waterfunservice.service.post.PostService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
@Validated
public class PostController {
    private final PostService postService;
    private final PostMapper postMapper;




    /***
     * Get All the posts by page and optional params;
     * @param categoryId category id
     * @param tagIds tag ids belong to the post
     * @return  page ofPending posts
     */
    @GetMapping("/list")
    public ApiResponse<Page<PostCardResp>> listPublicPosts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false)List<Integer> tagIds,
            @PageableDefault() Pageable pageable
            ){

        Specification<Post> spec = PostSpec.ofPublic(categoryId, tagIds, null);
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
            @PageableDefault() Pageable pageable
    ){
        Specification<Post> spec = PostSpec.ofSelf(status,visibility, categoryId, tagIds);
        return ApiResponse.success(postService.listAuthorCardPosts(spec, pageable));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id){
        postService.deletePost(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> editPost(@PathVariable Long id, @Valid @RequestBody PutUserPostReq req){
        postService.updatePost(id, req);
        return ApiResponse.success();
    }

    @PostMapping("/draft")
    public ApiResponse<Long> draftNewPost(){
        return ApiResponse.success(postService.draftNew());
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publishPost(@PathVariable Long id){
        postService.publish(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/temp-save")
    public ApiResponse<Void> tempSavePost(@PathVariable Long id, PostSaveReq req){
        postService.tempSave(id, req);
        return ApiResponse.success();
    }
}
