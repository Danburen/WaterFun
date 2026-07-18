package org.waterwood.waterfunadminservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.ModerationBaseQuery;
import org.waterwood.waterfunadminservice.api.request.AuditResponse;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.ModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.response.ModerationStatsResp;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunadminservice.api.response.content.audit.UserAuditStats;
import org.waterwood.waterfunadminservice.service.ModerationService;
import org.waterwood.waterfunservicecore.api.moderation.ImageAuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.PostAuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.ReplyPayload;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

import java.util.List;

@RestController
@RequestMapping("/api/admin/moderations")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final MessageSource messageSource;

    @GetMapping("/list/posts")
    public ApiResponse<Page<AuditResponse<PostAuditPayload>>> listPosts(
           ModerationBaseQuery query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size){
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        return ApiResponse.success(
                moderationService.listPendingPostTasks(query, pageable)
        );
    }

    @GetMapping("/list/images")
    public ApiResponse<Page<AuditResponse<ImageAuditPayload>>> listImages(
            ModerationBaseQuery query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        return ApiResponse.success(
                moderationService.listPendingImageTasks(query, pageable)
        );
    }

    @GetMapping("/list/texts")
    public ApiResponse<Page<AuditResponse<ReplyPayload>>> listTexts(
            ModerationBaseQuery query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        return ApiResponse.success(
                moderationService.listPendingTextTasks(query, pageable)
        );
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<AuditResponse<PostAuditPayload>> getPost(@PathVariable Long id){
        return ApiResponse.success(moderationService.getPostTask(id));
    }

    @GetMapping("/images/{id}")
    public ApiResponse<AuditResponse<ImageAuditPayload>> getImage(@PathVariable Long id){
        return ApiResponse.success(moderationService.getImageTask(id));
    }

    @GetMapping("/texts/{id}")
    public ApiResponse<AuditResponse<ReplyPayload>> getText(@PathVariable Long id){
        return ApiResponse.success(moderationService.getTextTask(id));
    }

//    @GetMapping("/{id}/resources")
//    public ApiResponse<List<ModerationResourceRes>> listResourcesByTask(@PathVariable Long id) {
//        return ApiResponse.success(moderationService.listTaskResources(id));
//    }
//
//    @GetMapping("/{taskId}/resources/{resourceUuid}")
//    public ApiResponse<ModerationResourceRes> getResource(@PathVariable Long taskId, @PathVariable String resourceUuid) {
//        return ApiResponse.success(moderationService.getTaskResource(taskId, resourceUuid));
//    }
//
//    @PostMapping("/{taskId}/resources/{resourceUuid}/approve")
//    public ApiResponse<Void> approveResource(@PathVariable Long taskId, @PathVariable String resourceUuid) {
//        moderationService.approveResource(taskId, resourceUuid);
//        return ApiResponse.success();
//    }
//
//    @PostMapping("/{taskId}/resources/{resourceUuid}/reject")
//    public ApiResponse<Void> rejectResource(@PathVariable Long taskId, @PathVariable String resourceUuid, @RequestBody @Valid ModerateRejectRequest req) {
//        moderationService.rejectResource(taskId, resourceUuid, req);
//        return ApiResponse.success();
//    }

    @PostMapping("/approve")
    public ApiResponse<BatchResult> approveAll(@RequestBody @Valid BatchModerateRequest req){
        return ApiResponse.success(
                moderationService.approveAll(req)
        );
    }

    @PostMapping("/reject")
    public ApiResponse<BatchResult> rejectAll(@RequestBody @Valid BatchModerateRejectRequest req){
        return ApiResponse.success(
                moderationService.rejectAll(req)
        );
    }

    @Operation(
            summary = "审核通过任务",
            description = "将任务下所有待审资源标记为通过。若存在已被标记为可疑或驳回的资源，则任务不通过，并返回这些可疑资源列表。"
    )
    @PostMapping("/{id}/approve")
    public ApiResponse<List<ModerationResourceRes>> approve(@PathVariable Long id){
        List<ModerationResourceRes> res = moderationService.approve(id);
        return res.isEmpty() ?
                ApiResponse.success() :
                ApiResponse.reject(
                        BaseResponseCode.AUDIT_TASK_RESOURCE_REJECT_OR_SUSPECT.getCode(),
                        messageSource.getMessage(
                                BaseResponseCode.AUDIT_TASK_RESOURCE_REJECT_OR_SUSPECT.getCode(),
                                null,
                                null
                        ),
                        res
                );
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<BatchResult> reject(@PathVariable Long id, @RequestBody @Valid ModerateRejectRequest req){
        moderationService.reject(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/stats")
    public ApiResponse<ModerationStatsResp> getStats(
            @RequestParam(required = false) TargetType targetType) {
        return ApiResponse.success(moderationService.getModerationStats(targetType));
    }

    @GetMapping("/user-audit-stats")
    public ApiResponse<UserAuditStats> getUserAuditStats(@RequestParam Long userId) {
        return ApiResponse.success(moderationService.getUserAuditStats(userId));
    }
}
