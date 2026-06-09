package org.waterwood.waterfunservicecore.entity.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.audit.AuditLog;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogSpec {
    public static Specification<AuditLog> of(
            Long userId,
            String username,
            AuditLogActionType action,
            String ip,
            AuditLogStatusType status,
            Instant createdAtStart,
            Instant createdAtEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (StringUtil.isNotBlank(username)) {
                predicates.add(cb.like(root.get("username"), "%" + username.trim() + "%"));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (StringUtil.isNotBlank(ip)) {
                predicates.add(cb.like(root.get("ip"), "%" + ip.trim() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (createdAtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtStart));
            }
            if (createdAtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdAtEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
