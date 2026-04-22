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
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.ModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.response.ModerateTaskResponse;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunadminservice.service.ModerationService;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditResourceType;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditTaskResource;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.task.AduitTaskType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.AuditTaskResourceSpec;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.AuditTaskSpec;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/moderations")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/list")
    public ApiResponse<Page<ModerateTaskResponse>> list(@RequestParam(required = false) AduitTaskType taskType,
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
            @RequestParam(required = false) AuditResourceType resourceType,
            @RequestParam(required = false) Long auditorId,
            @RequestParam(required = false) Instant auditAtStart,
            @RequestParam(required = false) Instant auditAtEnd,
            @PageableDefault Pageable pageable
    ) {
        Specification<AuditTaskResource> spec = AuditTaskResourceSpec.of(
                taskId,
                status,
                resourceType,
                auditorId,
                auditAtStart,
                auditAtEnd
        );
        return ApiResponse.success(moderationService.listResourcesWithPayload(spec, pageable));
    }

    @GetMapping("/{id}/resources")
    public ApiResponse<List<ModerationResourceRes>> listResourcesByTask(@PathVariable Long id) {
        return ApiResponse.success(moderationService.listTaskResources(id));
    }

    @GetMapping("/resources/{resourceId}")
    public ApiResponse<ModerationResourceRes> getResource(@PathVariable Long resourceId) {
        return ApiResponse.success(moderationService.getTaskResource(resourceId));
    }

    @PostMapping("/resources/{resourceId}/approve")
    public ApiResponse<Void> approveResource(@PathVariable Long resourceId) {
        moderationService.approveResource(resourceId);
        return ApiResponse.success();
    }

    @PostMapping("/resources/{resourceId}/reject")
    public ApiResponse<Void> rejectResource(@PathVariable Long resourceId, @RequestBody @Valid ModerateRejectRequest req) {
        moderationService.rejectResource(resourceId, req);
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

    @PostMapping("/{id}/approve")
    public ApiResponse<BatchResult> approve(@PathVariable Long id){
        moderationService.approve(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<BatchResult> reject(@PathVariable Long id, @RequestBody @Valid ModerateRejectRequest req){
        moderationService.reject(id, req);
        return ApiResponse.success();
    }
}
