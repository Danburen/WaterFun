package org.waterwood.waterfunservicecore.services.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.api.moderation.ReportPayload;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.audit.*;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.resource.AuditResourceId;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.ReportAlreadyExistException;
import org.waterwood.waterfunservicecore.exception.ReportNotFoundException;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.IdGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContentAuditServiceImpl implements ContentAuditService {
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final ResourceRepository resourceRepository;
    private final AuditTaskRepository auditTaskRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public void handleUserSubmit(Serializable targetId, TargetType targetType, AuditPayload payload, List<String> resourceUuids) {
        String stringTargetId = targetId.toString();
        AuthContext ctx = UserCtxHolder.safeGet()
                .orElseThrow(() -> new ServiceException("User submit audit task must be user"));
        AuditTask task = auditTaskRepository
                .findByTargetIdAndTargetTypeAndStatus(stringTargetId, targetType, AuditStatus.PENDING)
                .orElseGet(() -> {
                    AuditTask newTask = new AuditTask();
                    newTask.setId(IdGenerator.generateAuditTaskId());
                    newTask.setTargetId(stringTargetId);
                    newTask.setSubmitAt(Instant.now());
                    newTask.setTargetType(targetType);
                    newTask.setPriority(Priority.MEDIUM);
                    newTask.setFormat(payload.getFormat());
                    newTask.setTriggerType(AuditTriggerType.USER_SUBMIT);
                    newTask.setSubmitter(userRepository.getReferenceById(ctx.getUserUid()));
                    return newTask;
                });

        task.setStatus(AuditStatus.PENDING);
        task.setPayload(payload.toJson());
        task.setUpdatedAt(Instant.now());
        auditTaskRepository.save(task);
        synchronizeTaskResources(task, resourceUuids);
    }

    @Transactional
    @Override
    public Long handleUserReport(Serializable targetId, TargetType targetType, AuditType type, String reason) {
        String stringTargetId = targetId.toString();
        AuthContext ctx = UserCtxHolder.safeGet()
                .orElseThrow(() -> new ServiceException("User report must be authenticated"));

        auditTaskRepository.findByTargetIdAndTargetTypeAndSubmitterUidAndStatus(
                stringTargetId, targetType, ctx.getUserUid(), AuditStatus.PENDING
        ).ifPresent(task -> {
            throw new ReportAlreadyExistException();
        });

        AuditTask task = new AuditTask();
        task.setId(IdGenerator.generateAuditTaskId());
        task.setTargetId(stringTargetId);
        task.setSubmitAt(Instant.now());
        task.setTargetType(targetType);
        task.setPriority(Priority.MEDIUM);
        task.setFormat(AuditContentFormat.TXT);
        task.setTriggerType(AuditTriggerType.USER_REPORT);
        task.setSubmitter(userRepository.getReferenceById(ctx.getUserUid()));
        task.setStatus(AuditStatus.PENDING);
        task.setPayload(new ReportPayload(type, reason, ctx.getUserUid()).toJson());
        auditTaskRepository.save(task);
        return task.getId();
    }

    @Transactional
    @Override
    public void cancelUserReport(Serializable targetId, TargetType targetType) {
        String stringTargetId = targetId.toString();
        AuthContext ctx = UserCtxHolder.safeGet()
                .orElseThrow(() -> new ServiceException("User cancel report must be authenticated"));

        AuditTask task = auditTaskRepository
                .findByTargetIdAndTargetTypeAndSubmitterUidAndStatus(
                        stringTargetId, targetType, ctx.getUserUid(), AuditStatus.PENDING
                ).orElseThrow(ReportNotFoundException::new);

        task.setStatus(AuditStatus.CANCELED);
        task.setUpdatedAt(Instant.now());
        auditTaskRepository.save(task);
    }

    @Transactional
    @Override
    public Long createAuditTask(AuditTriggerType triggerType, Serializable targetId, TargetType targetType,
                                AuditContentFormat format, AuditPayload payload, Long submitterUid) {
        String stringTargetId = targetId != null ? targetId.toString() : null;
        AuditTask task = new AuditTask();
        task.setId(IdGenerator.generateAuditTaskId());
        task.setTargetId(stringTargetId);
        task.setSubmitAt(Instant.now());
        task.setTargetType(targetType);
        task.setPriority(Priority.MEDIUM);
        task.setFormat(format);
        task.setTriggerType(triggerType);
        task.setSubmitter(userRepository.getReferenceById(submitterUid));
        task.setStatus(AuditStatus.PENDING);
        task.setPayload(payload.toJson());
        auditTaskRepository.save(task);
        return task.getId();
    }

    @Override
    public List<Resource> getLinkedResources(Serializable targetId, TargetType targetType) {
        return auditTaskResourceRepository
                .findByTaskTargetIdAndTaskTargetType(targetId.toString(), targetType);
    }

    @Override
    public List<String> getLinkedResourcesUuids(Serializable targetId, TargetType targetType) {
        return auditTaskResourceRepository
                .findResourceUuidByTaskTargetIdAndTaskTargetType(targetId.toString(), targetType);
    }

    @Override
    public void synchronizeTaskResources(Long taskId, List<String> resourceUuids) {
        AuditTask task = auditTaskRepository.getReferenceById(taskId);
        synchronizeTaskResources(task, resourceUuids);
    }

    private void synchronizeTaskResources(AuditTask task, List<String> newUuids) {
        List<AuditResource> existing = auditTaskResourceRepository.findAllByTaskId(task.getId());
        Set<String> existingUuids = existing.stream()
                .map(ar -> ar.getResource().getUuid())
                .collect(Collectors.toSet());

        List<String> toAdd = newUuids.stream()
                .filter(uuid -> !existingUuids.contains(uuid))
                .toList();

        List<AuditResource> toRemove = existing.stream()
                .filter(ar -> !newUuids.contains(ar.getResource().getUuid()))
                .toList();
        if (!toAdd.isEmpty()) {
            List<AuditResource> newResources = resourceRepository
                    .findByUuidInAndStatus(toAdd, ResourceStatus.ACTIVE)
                    .stream()
                    .map(res -> {
                        AuditResource ar = new AuditResource();
                        AuditResourceId arId = new AuditResourceId();
                        arId.setResourceUuid(res.getUuid());
                        arId.setTaskId(task.getId());
                        ar.setId(arId);
                        ar.setTask(task);
                        ar.setResource(res);
                        return ar;
                    })
                    .toList();
            auditTaskResourceRepository.saveAll(newResources);
        }

        if (!toRemove.isEmpty()) {
            auditTaskResourceRepository.deleteAll(toRemove);
        }
    }
}
