package org.waterwood.waterfunservicecore.services.audit;

import org.waterwood.waterfunservicecore.api.moderation.AuditPayload;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
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
}
