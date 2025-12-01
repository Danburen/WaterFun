package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.Role;

import java.util.ArrayList;
import java.util.List;

public final class RoleSpec{
    public static Specification<Role> of(String name, Integer parentId){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> preds = new ArrayList<>();
            if(name != null){
                preds.add(criteriaBuilder.equal(root.get("name"), name));
            }

            if(parentId != null){
                preds.add(criteriaBuilder.equal(root.get("parent").get("id"), parentId));
            }
            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }
}
