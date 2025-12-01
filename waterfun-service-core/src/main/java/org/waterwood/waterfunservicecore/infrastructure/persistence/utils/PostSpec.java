package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.enums.PostStatus;
import org.waterwood.api.enums.PostVisibility;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.infrastructure.security.AuthContextHelper;

import java.util.ArrayList;
import java.util.List;

public final class PostSpec {
    public static Specification<Post> ofPublic(Integer categoryId, List<Integer> tagIds, Long authorId){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> preds = new ArrayList<>();
            // Must public and visible to all
            preds.add(criteriaBuilder.equal(root.get("status"), PostStatus.PUBLISHED));
            preds.add(criteriaBuilder.equal(root.get("visibility"), PostVisibility.PUBLIC));

            if (categoryId != null) {
                preds.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                Join<Post, Tag> tagJoin = root.join("tags");
                preds.add(tagJoin.get("id").in(tagIds));
            }

            if (authorId != null) {
                preds.add(criteriaBuilder.equal(root.get("author").get("id"), authorId));
            }

            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }

    public static Specification<Post> ofSelf(PostStatus status, PostVisibility visibility, Integer categoryId, List<Integer> tagIds){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> preds = new ArrayList<>();
            if (status != null) {
                preds.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (visibility != null) {
                preds.add(criteriaBuilder.equal(root.get("visibility"), visibility));
            }

            if (categoryId != null) {
                preds.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                Join<Post, Tag> tagJoin = root.join("tags");
                preds.add(tagJoin.get("id").in(tagIds));
            }

            // Current user id
            preds.add(criteriaBuilder.equal(root.get("author").get("id"), AuthContextHelper.getCurrentUserId()));
            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }
}
