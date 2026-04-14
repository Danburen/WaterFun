package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TagSpec {

    public static Specification<Tag> of(String name, String slug, Long creatorId, Instant createdStart, Instant createdEnd) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if(StringUtil.isNotBlank(name)) {
                preds.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if(StringUtil.isNotBlank(slug)) {
                preds.add(cb.like(root.get("slug"), "%" + slug + "%"));
            }
            if(creatorId != null) {
                preds.add(cb.equal(root.get("creator").get("id"), creatorId));
            }
            if(createdStart != null) {
                preds.add(cb.greaterThan(root.get("createdAt"), createdStart));
            }
            if(createdEnd != null) {
                preds.add(cb.lessThan(root.get("createdAt"), createdEnd));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
