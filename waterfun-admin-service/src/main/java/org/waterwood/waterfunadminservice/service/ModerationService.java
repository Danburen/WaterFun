package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.ModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.response.ModerateTaskResponse;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditTaskResource;

import java.util.List;

public interface ModerationService {
    /**
     * List audit tasks by specification and pageable
     * @param spec specification
     * @param pageable pageable
     * @return page of AuditTask entity
     */
    Page<AuditTask> listTasks(Specification<AuditTask> spec, Pageable pageable);

    /**
     * List tasks and assemble moderation payload (resource urls/rendered content).
     */
    Page<ModerateTaskResponse> listTasksWithPayload(Specification<AuditTask> spec, Pageable pageable);

    /**
     * List resources for one task.
     */
    List<ModerationResourceRes> listTaskResources(Long taskId);

    /**
     * List resources by specification and pageable with assembled payload fields.
     */
    Page<ModerationResourceRes> listResourcesWithPayload(Specification<AuditTaskResource> spec, Pageable pageable);

    /**
     * Get one audit resource detail.
     */
    ModerationResourceRes getTaskResource(Long resourceId);

    /**
     * Batch approve audit tasks by ids in request, and return the result of batch operation
     * @param req request body
     * @return batch result of the request.
     */
    BatchResult approveAll(BatchModerateRequest req);

    /**
     * Batch reject audit tasks by ids in request, and return the result of batch operation
     * @param req request body
     * @return batch result of the request.
     */
    BatchResult rejectAll(BatchModerateRejectRequest req);

    /**
     * Approve a audit task
     * @param id target audit task id.
     * @throws org.waterwood.waterfunservicecore.exception.NotFoundException if the audit task is not found or not in pending status.
     */
    void approve(Long id);

    /**
     * Reject a audit task
     *
     * @param id  id target audit task id.
     * @param req request body
     * @throws org.waterwood.waterfunservicecore.exception.NotFoundException if the audit task is not found or not in pending status.
     */
    void reject(Long id, ModerateRejectRequest req);

    /**
     * Approve one task resource then aggregate task status.
     */
    void approveResource(Long resourceId);

    /**
     * Reject one task resource then aggregate task status.
     */
    void rejectResource(Long resourceId, ModerateRejectRequest req);
}
