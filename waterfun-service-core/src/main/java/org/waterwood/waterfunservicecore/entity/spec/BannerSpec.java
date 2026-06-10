package org.waterwood.waterfunservicecore.entity.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.content.Banner;
import org.waterwood.waterfunservicecore.entity.content.BannerPosition;
import org.waterwood.waterfunservicecore.entity.content.VisibleStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class BannerSpec {

    public static Specification<Banner> of(String title,
                                           String subtitle,
                                           BannerPosition position,
                                           VisibleStatus status,
                                           Instant startAt,
                                           Instant endAt, Boolean isDeleted) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (StringUtil.isNotBlank(title)) {
                preds.add(cb.like(root.get("title"), "%" + title + "%"));
            }
            if (StringUtil.isNotBlank(subtitle)) {
                preds.add(cb.like(root.get("subtitle"), "%" + subtitle + "%"));
            }
            if (position != null) {
                preds.add(cb.equal(root.get("position"), position));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (startAt != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("startAt"), startAt));
            }
            if (endAt != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("endAt"), endAt));
            }
            if (isDeleted != null) {
                preds.add(cb.equal(root.get("isDeleted"), isDeleted));
            } else {
                preds.add(cb.isFalse(root.get("isDeleted")));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}

