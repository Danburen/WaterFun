package org.waterwood.waterfunservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.request.CreateCommentReq;
import org.waterwood.waterfunservice.api.request.CreateReportReq;
import org.waterwood.waterfunservice.api.response.CommentResponse;
import org.waterwood.waterfunservice.api.response.ReportResponse;
import org.waterwood.waterfunservice.service.post.CommentService;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.aspect.BanCheck;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.audit.ContentAuditService;
import org.waterwood.waterfunservice.service.report.ReportService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    private final ReportService reportService;
    private final ContentAuditService contentAuditService;

    @Operation(summary = "List root comments of a post with cursor pagination")
    @GetMapping("/list")
    public ApiResponse<CursorPage<CommentResponse, String>> listPostComments(
            @RequestParam Long postId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) Long includeRootId) {
        return ApiResponse.success(commentService.listRootComments(postId, cursor, limit, includeRootId));
    }

    @GetMapping("/{rootId}/replies")
    public ApiResponse<CursorPage<CommentResponse, Long>> listReplies(
            @PathVariable Long rootId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) Long includeRootId) {

        return ApiResponse.success(commentService.listReplies(rootId, cursor, limit, includeRootId));
    }

    @GetMapping("/{commentId}")
    public ApiResponse<CommentResponse> getComment(@PathVariable Long commentId) {
       return ApiResponse.success( commentService.getComment(commentId));
    }

    @BanCheck("ban:comment")
    @PostMapping
    public ApiResponse<Void> postComment(@RequestBody @Valid CreateCommentReq req){
        commentService.create(req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id){
        commentService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Void> likeComment(@PathVariable Long id){
        commentService.like(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/report")
    public ApiResponse<ReportResponse> reportComment(@PathVariable Long id, @Valid @RequestBody CreateReportReq req) {
        AuthContext ctx = UserCtxHolder.safeGet()
                .orElseThrow(() -> new ServiceException("Authentication required"));
        Long taskId = reportService.submitReport(
                String.valueOf(id),
                TargetType.COMMENT,
                req.getType(),
                req.getReason(),
                null
        );
        return ApiResponse.success(new ReportResponse(taskId));
    }

    @PostMapping("/{id}/report/cancel")
    public ApiResponse<Void> cancelReportComment(@PathVariable Long id) {
        contentAuditService.cancelUserReport(id, TargetType.COMMENT);
        return ApiResponse.success();
    }
}
