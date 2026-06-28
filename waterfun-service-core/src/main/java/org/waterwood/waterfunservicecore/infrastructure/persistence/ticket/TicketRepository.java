package org.waterwood.waterfunservicecore.infrastructure.persistence.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.ticket.Ticket;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    Optional<Ticket> findByIdAndStatus(Long id, TicketAuditStatus status);

    @Query("SELECT t.ticketType, COUNT(t) FROM Ticket t WHERE t.submitter.uid = :userUid GROUP BY t.ticketType")
    List<Object[]> countByTicketTypeAndUserUid(@Param("userUid") Long userUid);

    @Query("SELECT t FROM Ticket t WHERE t.submitter.uid = :submitterUid AND t.targetId = :targetId " +
            "AND t.targetType = :targetType AND t.ticketType = :ticketType AND t.status = :status")
    Optional<Ticket> findBySubmitterUidAndTargetIdAndTargetTypeAndTicketTypeAndStatus(
            @Param("submitterUid") Long submitterUid, @Param("targetId") String targetId,
            @Param("targetType") TargetType targetType, @Param("ticketType") TicketType ticketType,
            @Param("status") TicketAuditStatus status);

    @Query("SELECT t.ticketType, COUNT(t) FROM Ticket t GROUP BY t.ticketType")
    List<Object[]> countByTicketType();
}
