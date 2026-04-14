package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CategorySpec {
    public static Specification<Category> of(String name, String slug, Integer parentId, Long creatorId, Instant createStart, Instant createEnd) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if(StringUtil.isNotBlank(name)){
                preds.add(cb.like(root.get("name"), "%"+name+"%"));
            }

            if(StringUtil.isNotBlank(slug)){
                preds.add(cb.like(root.get("slug"), "%"+slug+"%"));
            }

            if(parentId != null){
                preds.add(cb.equal(root.get("parent").get("id"), parentId));
            }

            if(creatorId != null){
                preds.add(cb.equal(root.get("creator").get("id"), creatorId));
            }

            if(createStart != null){
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createStart));
            }

            if(createEnd != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), createEnd));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
