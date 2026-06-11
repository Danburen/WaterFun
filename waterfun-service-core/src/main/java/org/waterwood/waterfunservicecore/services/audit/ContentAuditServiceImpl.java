package org.waterwood.waterfunservicecore.services.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.audit.*;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.resource.AuditResourceId;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
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
                .findByTargetIdAndTargetTypeAndStatus(stringTargetId, TargetType.POST, AuditStatus.PENDING)
                .orElseGet(() -> {
                    AuditTask newTask = new AuditTask();
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
        task.setUserLocale(ctx.getLocale().getLanguage());
        task.setPayload(payload.toJson());
        auditTaskRepository.save(task);
        synchronizeTaskResources(task, resourceUuids);
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
