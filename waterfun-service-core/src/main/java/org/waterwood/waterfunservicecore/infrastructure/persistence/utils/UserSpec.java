package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.user.UserFollower;

import java.util.ArrayList;
import java.util.List;

public final class UserSpec {
    public static Specification<UserFollower> ofUserFollowers(Long userUid){
       return (root, query, criteriaBuilder) ->{
           List<Predicate> preds = new ArrayList<>();
           preds.add(criteriaBuilder.equal(root.get("userUid").get("uid"), userUid));
           return criteriaBuilder.and(preds.toArray(new Predicate[0]));
       };
    }

}
