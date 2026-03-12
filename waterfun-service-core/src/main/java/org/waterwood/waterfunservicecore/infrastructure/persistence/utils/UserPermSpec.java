package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;

import java.util.ArrayList;
import java.util.List;

public final class UserPermSpec {
    public static Specification<UserPermission> of(long userId, String name, String code, String resource, PermissionType type, Integer parentId){
        return (root, query, criteriaBuilder) ->{
            List<Predicate> preds = new ArrayList<>();

            preds.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            if (StringUtil.isNotBlank(name)) {
                preds.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }

            if (StringUtil.isNotBlank(code)) {
                preds.add(criteriaBuilder.like(root.get("code"), "%" + code + "%"));
            }

            if (StringUtil.isNotBlank(resource)) {
                preds.add(criteriaBuilder.like(root.get("resource"), "%" + resource + "%"));
            }

            if (type != null) {
                preds.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (parentId != null) {
                preds.add(criteriaBuilder.equal(root.get("parent"), parentId));
            }
            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }
}
