package org.waterwood.waterfunservicecore.services.user;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.user.UserRole;

import java.util.ArrayList;
import java.util.List;

public final class UserRoleSpec {
    public static Specification<UserRole> of(Long uid, String roleName, Integer roleParent) {
        return (root, query, criteriaBuilder) ->{
            List<Predicate> preds = new ArrayList<>();

            if(uid != null){
                preds.add(criteriaBuilder.equal(root.get("user").get("uid"), uid));
            }

            if(StringUtil.isNotBlank(roleName)){
                preds.add(criteriaBuilder.like(root.get("role").get("name"), "%" + roleName + "%"));
            }

            if(roleParent != null){
                preds.add(criteriaBuilder.equal(root.get("role").get("parent").get("id"), roleParent));
            }

            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }
}
