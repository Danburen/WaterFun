package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.task.AduitTaskType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditTaskSpec {
    public static Specification<AuditTask> ofPending(AduitTaskType taskType, Long submitterId, Instant submitAtStart, Instant submitAtEnd) {
        return (root, query, cb) -> {
           List<Predicate> predicates = new ArrayList<>();
           predicates.add(cb.equal(root.get("status"), 1));
           if(taskType != null) {
               predicates.add(cb.equal(root.get("taskType"), taskType));
           }

           if(submitterId != null) {
               predicates.add(cb.equal(root.get("submitter").get("uid"), submitterId));
           }

           if(submitAtStart != null) {
               predicates.add(cb.greaterThanOrEqualTo(root.get("submitAt"), submitAtStart));
           }

           if(submitAtEnd != null) {
               predicates.add(cb.lessThanOrEqualTo(root.get("submitAt"), submitAtEnd));
           }
           return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
