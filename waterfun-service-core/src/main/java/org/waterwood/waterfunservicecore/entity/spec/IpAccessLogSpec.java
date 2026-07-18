package org.waterwood.waterfunservicecore.entity.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.security.IpAccessLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class IpAccessLogSpec {

    public static Specification<IpAccessLog> of(
            String ip,
            Long userUid,
            String requestPath,
            String requestMethod,
            Integer httpStatus,
            String country,
            String province,
            String city,
            Instant createdAtStart,
            Instant createdAtEnd
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtil.isNotBlank(ip)) {
                predicates.add(cb.equal(root.get("ip"), ip.trim()));
            }
            if (userUid != null) {
                predicates.add(cb.equal(root.get("userUid"), userUid));
            }
            if (StringUtil.isNotBlank(requestPath)) {
                predicates.add(cb.like(root.get("requestPath"), "%" + requestPath.trim() + "%"));
            }
            if (StringUtil.isNotBlank(requestMethod)) {
                predicates.add(cb.equal(root.get("requestMethod"), requestMethod.trim().toUpperCase()));
            }
            if (httpStatus != null) {
                predicates.add(cb.equal(root.get("httpStatus"), httpStatus));
            }
            if (StringUtil.isNotBlank(country)) {
                predicates.add(cb.equal(root.get("country"), country.trim()));
            }
            if (StringUtil.isNotBlank(province)) {
                predicates.add(cb.equal(root.get("province"), province.trim()));
            }
            if (StringUtil.isNotBlank(city)) {
                predicates.add(cb.equal(root.get("city"), city.trim()));
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
