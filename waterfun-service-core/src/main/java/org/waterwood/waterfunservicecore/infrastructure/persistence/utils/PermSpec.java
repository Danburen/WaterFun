package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.Permission;

import java.util.ArrayList;
import java.util.List;

public final class PermSpec {
    public static Specification<Permission> of(String name, String code, PermissionType type, String resource, Integer parentId){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> preds = new ArrayList<>();
            if(StringUtil.isNotBlank(name)){
                preds.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }

            if(StringUtil.isNotBlank(code)){
                preds.add(criteriaBuilder.like(root.get("code"), "%" + code + "%"));
            }

            if (type != null) {
                preds.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (StringUtil.isNotBlank(resource)) {
                preds.add(criteriaBuilder.equal(root.get("resource"), resource));
            }

            if(parentId != null){
                preds.add(criteriaBuilder.equal(root.get("parent").get("id"), parentId));
            }
            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }
}
