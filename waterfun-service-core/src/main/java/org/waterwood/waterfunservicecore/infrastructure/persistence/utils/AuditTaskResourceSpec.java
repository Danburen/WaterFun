package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditResourceType;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditTaskResource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditTaskResourceSpec {
    private AuditTaskResourceSpec() {
    }

    public static Specification<AuditTaskResource> of(
            Long taskId,
            AuditStatus status,
            AuditResourceType resourceType,
            Long auditorId,
            Instant auditAtStart,
            Instant auditAtEnd
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (taskId != null) {
                predicates.add(cb.equal(root.get("task").get("id"), taskId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (resourceType != null) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }
            if (auditorId != null) {
                predicates.add(cb.equal(root.get("auditor").get("uid"), auditorId));
            }
            if (auditAtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("auditAt"), auditAtStart));
            }
            if (auditAtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("auditAt"), auditAtEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

