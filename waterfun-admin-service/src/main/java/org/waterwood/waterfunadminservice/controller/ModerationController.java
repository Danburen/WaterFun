package org.waterwood.waterfunadminservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.ModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.response.ModerateTaskResponse;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunadminservice.service.ModerationService;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.common.io.ResourceType;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.AuditTaskResourceSpec;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.AuditTaskSpec;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/moderations")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final MessageSource messageSource;

    @GetMapping("/list")
    public ApiResponse<Page<ModerateTaskResponse>> list(@RequestParam(required = false) TargetType taskType,
                                                        @RequestParam(required = false) Long submitterId,
                                                        @RequestParam(required = false) Instant submitAtStart,
                                                        @RequestParam(required = false) Instant submitAtEnd,
                                                        @PageableDefault Pageable pageable){
        Specification<AuditTask> spec = AuditTaskSpec.ofPending(taskType, submitterId, submitAtStart, submitAtEnd);
        return ApiResponse.success(moderationService.listTasksWithPayload(spec, pageable));
    }

    @GetMapping("/resources/list")
    public ApiResponse<Page<ModerationResourceRes>> listResources(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) Long auditorId,
            @RequestParam(required = false) Instant auditAtStart,
            @RequestParam(required = false) Instant auditAtEnd,
            @PageableDefault Pageable pageable
    ) {
        Specification<AuditResource> spec = AuditTaskResourceSpec.of(
                taskId,
                status,
                resourceType,
                auditorId,
                auditAtStart,
                auditAtEnd
        );
        return ApiResponse.success(moderationService.listResourcesWithPayload(spec, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ModerateTaskResponse> getTask(@PathVariable Long id){
        return ApiResponse.success(
                moderationService.getTask(id)
        );
    }

    @GetMapping("/{id}/resources")
    public ApiResponse<List<ModerationResourceRes>> listResourcesByTask(@PathVariable Long id) {
        return ApiResponse.success(moderationService.listTaskResources(id));
    }

    @GetMapping("/{taskId}/resources/{resourceUuid}")
    public ApiResponse<ModerationResourceRes> getResource(@PathVariable Long taskId, @PathVariable String resourceUuid) {
        return ApiResponse.success(moderationService.getTaskResource(taskId, resourceUuid));
    }

    @PostMapping("/{taskId}/resources/{resourceUuid}/approve")
    public ApiResponse<Void> approveResource(@PathVariable Long taskId, @PathVariable String resourceUuid) {
        moderationService.approveResource(taskId, resourceUuid);
        return ApiResponse.success();
    }

    @PostMapping("/{taskId}/resources/{resourceUuid}/reject")
    public ApiResponse<Void> rejectResource(@PathVariable Long taskId, @PathVariable String resourceUuid, @RequestBody @Valid ModerateRejectRequest req) {
        moderationService.rejectResource(taskId, resourceUuid, req);
        return ApiResponse.success();
    }

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
}
