package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserFollower;

import java.time.Instant;
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

    public static Specification<User> of(String username, String nickname, String accountStatus, Instant createdStart, Instant createdEnd) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> preds = new ArrayList<>();
            if(StringUtil.isNotBlank(username)) {
                preds.add(criteriaBuilder.like(root.get("username"), "%" + username + "%"));
            }
            if(StringUtil.isNotBlank(nickname)) {
                preds.add(criteriaBuilder.like(root.get("nickname"), "%" + nickname + "%"));
            }

            if(StringUtil.isNotBlank(accountStatus)) {
                preds.add(criteriaBuilder.equal(root.get("accountStatus"), accountStatus));
            }else{
                //  default account is not deleted.
                preds.add(criteriaBuilder.notEqual(root.get("accountStatus"), "DELETED"));
            }

            if(createdStart != null) {
                preds.add(criteriaBuilder.greaterThan(root.get("createdAt"), createdStart));
            }
            if(createdEnd != null) {
                preds.add(criteriaBuilder.lessThan(root.get("createdAt"), createdEnd));
            }
            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }
}
