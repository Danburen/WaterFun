package org.waterwood.waterfunservicecore.infrastructure.persistence.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.ticket.TicketResource;
import org.waterwood.waterfunservicecore.entity.ticket.TicketResourceId;

import java.util.List;

public interface TicketResourceRepository extends JpaRepository<TicketResource, TicketResourceId> {
    List<TicketResource> findByIdTicketId(Long ticketId);
    List<TicketResource> findByIdTicketIdIn(List<Long> ticketIds);
}
