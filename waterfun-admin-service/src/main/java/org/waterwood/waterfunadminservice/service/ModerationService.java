package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.AuditResponse;
import org.waterwood.waterfunadminservice.api.request.ModerationBaseQuery;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.BatchModerateRequest;
import org.waterwood.waterfunadminservice.api.request.content.audit.ModerateRejectRequest;
import org.waterwood.waterfunadminservice.api.response.AuditTaskRes;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunservicecore.api.moderation.PostAuditPayload;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;

import java.util.List;

public interface ModerationService {

    /**
     * List tasks and assemble moderation payload (resource urls/rendered content).
     */
    Page<AuditTaskRes> listTasksWithPayload(Specification<AuditTask> spec, Pageable pageable);

    /**
     * List resources for one task.
     */
    List<ModerationResourceRes> listTaskResources(Long taskId);

    /**
     * List resources by specification and pageable with assembled payload fields.
     */
    Page<ModerationResourceRes> listResourcesWithPayload(Specification<AuditResource> spec, Pageable pageable);

    /**
     * Get one audit resource detail.
     */
    ModerationResourceRes getTaskResource(Long taskId, String resourceIUuid);

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
     *
     * @param id target audit task id.
     * @return if one of the task resources belong to the target task is suspect or rejected
     * then return the not passed task resources
     * @throws NotFoundException if the audit task is not found or not in pending status.
     */
    List<ModerationResourceRes> approve(Long id);

    /**
     * Reject a audit task
     *
     * @param id  id target audit task id.
     * @param req request body
     * @throws NotFoundException if the audit task is not found or not in pending status.
     */
    void reject(Long id, ModerateRejectRequest req);

    /**
     * Approve one task resource then aggregate task status.
     */
    void approveResource(Long taskId, String resourceUuid);

    /**
     * Reject one task resource then aggregate task status.
     */
    void rejectResource(Long taskId, String resourceUuid, ModerateRejectRequest req);

    /**
     * Return a moderation task response
     * @param id target task id
     * @return ModerationTaskResponse of target post
     */
    AuditTaskRes getTask(Long id);

    /**
     * List post audit tasks with payload assembled.
     * @param query {@link ModerationBaseQuery} query params
     * @param pageable pageable
     * @return page of {@link AuditResponse<PostAuditPayload>}
     */
    Page<AuditResponse<PostAuditPayload>> listPendingPostTasks(ModerationBaseQuery query, Pageable pageable);
}
