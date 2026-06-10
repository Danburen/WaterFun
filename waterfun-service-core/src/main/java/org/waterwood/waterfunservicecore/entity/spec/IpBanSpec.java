package org.waterwood.waterfunservicecore.entity.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.security.IpBan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class IpBanSpec {
    public static Specification<IpBan> of(
            String ip,
            String reason,
            Instant bannedAtStart,
            Instant bannedAtEnd,
            Instant expiresAtStart,
            Instant expiresAtEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(StringUtil.isNotBlank(ip)) {
                predicates.add(cb.like(root.get("ip"), "%" + ip.trim() + "%"));
            }
            if(StringUtil.isNotBlank(reason)) {
                predicates.add(cb.like(root.get("reason"), "%" + reason.trim() + "%"));
            }
            if(bannedAtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bannedAt"), bannedAtStart));
            }
            if(bannedAtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bannedAt"), bannedAtEnd));
            }

            if(expiresAtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expiresAt"), expiresAtStart));
            }

            if(expiresAtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expiresAt"), expiresAtEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
