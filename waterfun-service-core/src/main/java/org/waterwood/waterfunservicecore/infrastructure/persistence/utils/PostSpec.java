package org.waterwood.waterfunservicecore.infrastructure.persistence.utils;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.enums.PostStatus;
import org.waterwood.api.enums.PostVisibility;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

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

            // Current user uid
            preds.add(criteriaBuilder.equal(root.get("author").get("uid"), UserCtxHolder.getUserUid()));
            return criteriaBuilder.and(preds.toArray(new Predicate[0]));
        };
    }

    public static Specification<Post> of(String title, PostStatus status, Integer categoryId, Long authorId, List<Integer> tagIds, String slug) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (StringUtil.isNotBlank(title)) {
                preds.add(cb.like(root.get("title"), "%" + title + "%"));
            }
            if (StringUtil.isNotBlank(slug)) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                preds.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (authorId != null) {
                preds.add(cb.equal(root.get("author").get("id"), authorId));
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                Subquery<Long> hitCountSub = query.subquery(Long.class);
                Root<Post> subRoot = hitCountSub.from(Post.class);
                Join<Post, Tag> subTag = subRoot.join("tags");
                // order with hit counts ofPending tags instead ofPending normal order.
                hitCountSub.select(cb.count(subTag.get("id")))
                        .where(
                                cb.equal(subRoot.get("id"), root.get("id")),
                                subTag.get("id").in(tagIds)
                        );
                query.orderBy(cb.desc(hitCountSub));
                query.distinct(true);
                Join<Post, Tag> tagJoin = root.join("tags", JoinType.INNER);
                return tagJoin.get("id").in(tagIds);
            }
            if (slug != null) {
                preds.add(cb.equal(root.get("slug"), slug));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
