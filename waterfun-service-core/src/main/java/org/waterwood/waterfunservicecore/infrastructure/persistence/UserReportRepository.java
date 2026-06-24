package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;
import org.waterwood.waterfunservicecore.entity.ticket.UserTicket;
import org.waterwood.waterfunservicecore.entity.ticket.UserTicketId;

import java.util.Optional;

public interface UserReportRepository extends JpaRepository<UserTicket, UserTicketId> {

    @Query("SELECT ut FROM UserTicket ut WHERE ut.id.ticketId = :ticketId AND ut.id.userUid = :userUid")
    Optional<UserTicket> findByTicketIdAndUserUid(@Param("ticketId") Long ticketId, @Param("userUid") Long userUid);

    @Query("SELECT ut FROM UserTicket ut WHERE ut.id.ticketId = :ticketId")
    Optional<UserTicket> findByTicketId(@Param("ticketId") Long ticketId);
}
