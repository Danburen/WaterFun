package org.waterwood.waterfunservicecore.entity.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.ticket.Ticket;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TicketSpec {

    public static Specification<Ticket> of(
            List<TicketType> ticketTypes,
            TicketAuditStatus status,
            Long submitterUid,
            String targetId,
            Instant createdAtStart,
            Instant createdAtEnd
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ticketTypes != null && !ticketTypes.isEmpty()) {
                predicates.add(root.get("ticketType").in(ticketTypes));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (submitterUid != null) {
                predicates.add(cb.equal(root.get("submitter").get("uid"), submitterUid));
            }
            if (targetId != null) {
                predicates.add(cb.equal(root.get("targetId"), targetId));
            }
            if (createdAtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtStart));
            }
            if (createdAtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdAtEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
