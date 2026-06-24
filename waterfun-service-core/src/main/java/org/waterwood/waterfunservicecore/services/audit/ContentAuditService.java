package org.waterwood.waterfunservicecore.services.audit;

import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.entity.audit.*;
import org.waterwood.waterfunservicecore.entity.resource.Resource;

import java.io.Serializable;
import java.util.List;

public interface ContentAuditService {
    /**
     * Handle user submit a task which need to audit
     * submitter is considered as current user.
     * @param targetId      {@link Serializable} target id which need to audit, e.g. post id, comment id, etc.
     * @param targetType    {@link TargetType} type of the target which need to audit
     * @param payload       {@link AuditPayload} audit payload which will to be stored to db in json type,
     * @param resourceUuids {@link List<String>} list of resource uuids associated to the target<b>(must be existed and activated)</b>
     */
    void handleUserSubmit(Serializable targetId, TargetType targetType, AuditPayload payload, List<String> resourceUuids);

    /**
     * Get target linked resource uuids
     * suppose to call before {@link #handleUserSubmit(Serializable, TargetType, AuditPayload, List)} to validate the resource uuids
     *
     * @param targetId   target id
     * @param targetType {@link TargetType} type of the target
     * @return {@link List} the list of linked resource uuids
     */
    List<Resource> getLinkedResources(Serializable targetId, TargetType targetType);
    List<String> getLinkedResourcesUuids(Serializable targetId, TargetType targetType);

    /**
     * Handle user report a target which need to audit
     * reporter is considered as current user.
     *
     * @param targetId   {@link Serializable} target id which need to audit, e.g. post id, comment id, etc.
     * @param targetType {@link TargetType} type of the target which need to audit
     * @param type       {@link AuditType} report category
     * @param reason     {@link String} optional custom reason for report
     * @return
     */
    Long handleUserReport(Serializable targetId, TargetType targetType, AuditType type, String reason);

    /**
     * Cancel current user's pending report for a target
     * @param targetId      {@link Serializable} target id
     * @param targetType    {@link TargetType} type of the target
     */
    void cancelUserReport(Serializable targetId, TargetType targetType);

    /**
     * Create an audit task for any trigger type
     * @param triggerType   {@link AuditTriggerType} trigger type
     * @param targetId      {@link Serializable} target id (nullable for no target)
     * @param targetType    {@link TargetType} target type
     * @param format        {@link AuditContentFormat} content format
     * @param payload       {@link AuditPayload} payload json
     * @param submitterUid  {@link Long} submitter user uid
     * @return {@link Long} created audit task id
     */
    Long createAuditTask(AuditTriggerType triggerType, Serializable targetId, TargetType targetType,
                         AuditContentFormat format, AuditPayload payload, Long submitterUid);

    /**
     * Synchronize resource bindings for an audit task.
     * Resources are persisted as audit evidence regardless of task status.
     * @param taskId        {@link Long} the audit task id
     * @param resourceUuids {@link List} resource uuids to bind
     */
    void synchronizeTaskResources(Long taskId, List<String> resourceUuids);

}
