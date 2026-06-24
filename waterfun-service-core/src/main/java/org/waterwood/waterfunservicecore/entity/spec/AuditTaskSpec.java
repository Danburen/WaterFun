package org.waterwood.waterfunservicecore.entity.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.audit.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditTaskSpec {

    public static Specification<AuditTask> of(
            AuditTriggerType triggerType,
            Priority priority,
            AuditStatus status,
            Long submitterUid,
            Instant submitAtStart,
            Instant submitAtEnd,
            TargetType targetType,
            AuditContentFormat format
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (triggerType != null) {
                predicates.add(cb.equal(root.get("triggerType"), triggerType));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (submitterUid != null) {
                predicates.add(cb.equal(root.get("submitter").get("uid"), submitterUid));
            }
            if (submitAtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("submitAt"), submitAtStart));
            }
            if (submitAtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("submitAt"), submitAtEnd));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            if (format != null) {
                predicates.add(cb.equal(root.get("format"), format));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
