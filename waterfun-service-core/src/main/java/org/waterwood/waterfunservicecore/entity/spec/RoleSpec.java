package org.waterwood.waterfunservicecore.entity.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.user.Role;

import java.util.ArrayList;
import java.util.List;

public final class RoleSpec{
    public static Specification<Role> of(String name, String code, Integer parentId){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> preds = new ArrayList<>();
            if(StringUtil.isNotBlank(name)){
                preds.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }

            if(StringUtil.isNotBlank(code)){
                preds.add(criteriaBuilder.like(root.get("code"), "%" + code + "%"));
            }

            if(parentId != null){
                preds.add(criteriaBuilder.equal(root.get("parent").get("id"), parentId));
            }
            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }
}
