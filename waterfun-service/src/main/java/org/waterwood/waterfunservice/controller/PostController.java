package org.waterwood.waterfunservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.ContentPreviewReq;
import org.waterwood.waterfunservice.api.request.CreateReportReq;
import org.waterwood.waterfunservice.api.request.PublicPostListReq;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservice.api.response.ReportResponse;
import org.waterwood.waterfunservice.api.response.post.*;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.aspect.BanCheck;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.entity.spec.PostSpec;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.audit.ContentAuditService;
import org.waterwood.waterfunservice.service.report.ReportService;
import org.waterwood.waterfunservice.service.post.PostService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
public class PostController {
    private final PostService postService;
    private final ReportService reportService;
    private final ContentAuditService contentAuditService;

    /***
     * Get All the posts by page and optional params;
     * @param req query params
     * @return  page of posts
     */
    @GetMapping("/list")
    @RateLimit(key = "listPublicPosts")
    public ApiResponse<Page<PostCardResp>> listPublicPosts(PublicPostListReq req){
        return ApiResponse.success(postService.listPublicCardPosts(req));
    }

    @GetMapping("/hot")
    public ApiResponse<Page<PostCardResp>> listHotPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.listHotPosts(PageRequest.of(Math.max(page - 1, 0), size)));
    }

    @RateLimit(key = "listPublicPosts")
    @GetMapping("/{id}")
    public ApiResponse<PostDetailResp> getPostDetail(@PathVariable Long id){
        return ApiResponse.success(
                postService.getPostDetail(id)
        );
    }
    @GetMapping("/me/list")
    public ApiResponse<Page<PostAuthorCardResp>> listMyPost(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) PostVisibility visibility,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false)List<Integer> tagIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        Specification<Post> spec = PostSpec.ofSelf(title, status, visibility, categoryId, tagIds);
        Sort sortBy = switch (sort) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt", "createdAt");
            case "most-viewed" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "most-liked" -> Sort.by(Sort.Direction.DESC, "likeCount");
            default -> Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt");
        };
        return ApiResponse.success(postService.listAuthorCardPosts(spec, PageRequest.of(page - 1, Math.min(size, 20), sortBy)));
    }

    @GetMapping("/me/stats")
    public ApiResponse<MyPostsStatsResp> getMyPostStats(){
        return ApiResponse.success(postService.getMyPostStats());
    }


    @BanCheck("ban:post")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id){
        postService.deletePost(id);
        return ApiResponse.success();
    }

    @BanCheck("ban:post")
    @PostMapping("/me/batch-delete")
    public ApiResponse<Void> batchDeletePost(@RequestBody List<Long> ids){
        postService.batchDelete(ids);
        return ApiResponse.success();
    }

    @BanCheck("ban:post")
    @PostMapping("/me/batch-publish")
    public ApiResponse<Void> batchPublishPost(@RequestBody List<Long> ids){
        postService.batchPublish(ids);
        return ApiResponse.success();
    }

    @BanCheck("ban:post")
    @PostMapping("/draft")
    public ApiResponse<Long> draftNewPost(){
        return ApiResponse.success(postService.draftNew());
    }

    @GetMapping("/{id}/edit")
    public ApiResponse<PostDraftResp> getEditPostDraft(@PathVariable Long id){
        return ApiResponse.success(postService.getEditPostDraft(id));
    }

    @Operation(summary = "Publish an existing draft post",
            description = "Publish the draft post specified by {id}. Returns empty body on success.")
    @BanCheck("ban:post")
    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publishPost(
            @Parameter(description = "Existing post ID to publish") @PathVariable Long id,
            @Valid @RequestBody PostSaveReq req){
        postService.publish(id, req);
        return ApiResponse.success();
    }

    @Operation(summary = "Create and publish a new post in one step",
            description = "Create a new post and publish it immediately. Returns the newly created post ID.")
    @BanCheck("ban:post")
    @PostMapping("/publish")
    public ApiResponse<Long> publicNewPost(@Valid @RequestBody PostSaveReq req){
        return ApiResponse.success(postService.publishNewPost(req));
    }

    @Operation(summary = "Preview content with post-bound resources",
            description = "Preview content, will replace Res://<uuid> to accessable url. " +
                    "Resources bound to the post or the user will be rendered. Requires an existing post {id}.")
    @PostMapping("/{id}/content/preview")
    public ApiResponse<String> previewContent(
            @Parameter(description = "Existing post ID for resource resolution") @PathVariable Long id,
            @Valid  @RequestBody ContentPreviewReq req){
        return ApiResponse.success(postService.contentPreview(id, req.getContent()));
    }

    @Operation(summary = "Preview content with user-bound resources only",
                description = "Preview content, will replace Res://<uuid> to accessable url. " +
                        "Only resources bound to the current user will be rendered, no post scope.")
    @PostMapping("/content/preview")
    public ApiResponse<String> previewContent(@RequestBody ContentPreviewReq req){
        return ApiResponse.success(postService.contentPreview(req.getContent()));
    }

    @Operation(summary = "Save changes to an existing draft",
            description = "Temporarily save content into an existing draft post identified by {id}. Returns empty body on success.")
    @PostMapping("/{id}/temp-save")
    public ApiResponse<Void> tempSavePost(@Parameter(description = "Existing draft post ID to save into") @PathVariable Long id, @Valid @RequestBody PostSaveReq req){
        postService.save(id, req);
        return ApiResponse.success();
    }

    @Operation(summary = "Create a new draft and save initial content",
            description = "Create a new draft post and save the provided content. Returns the newly created draft post ID.")
    @PostMapping("/temp-save")
    public ApiResponse<Long> tempSaveNewPost(@Valid @RequestBody PostSaveReq req){
        return ApiResponse.success(postService.saveNewPost(req));
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

    @Operation(summary = "Get users who liked a post")
    @GetMapping("/{id}/liked-users")
    public ApiResponse<List<UserBrief>> getLikedUsers(@PathVariable Long id) {
        return ApiResponse.success(postService.getLikedUsers(id));
    }

    @PostMapping("/{id}/report")
    public ApiResponse<ReportResponse> reportPost(@PathVariable Long id, @Valid @RequestBody CreateReportReq req) {
        AuthContext ctx = UserCtxHolder.safeGet()
                .orElseThrow(() -> new ServiceException("Authentication required"));
        postService.ensurePostReportable(id);
        Long taskId = reportService.submitReport(
                String.valueOf(id),
                TargetType.POST,
                req.getType(),
                req.getReason(),
                null
        );
        return ApiResponse.success(new ReportResponse(taskId));
    }

    @PostMapping("/{id}/report/cancel")
    public ApiResponse<Void> cancelReportPost(@PathVariable Long id) {
        contentAuditService.cancelUserReport(id, TargetType.POST);
        return ApiResponse.success();
    }

}
