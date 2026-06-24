package org.waterwood.waterfunadminservice.service.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunadminservice.api.request.ticket.TicketReviewRequest;
import org.waterwood.waterfunadminservice.api.response.ticket.TicketResponse;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.util.List;

public interface TicketModerationService {

    Page<TicketResponse> listTickets(List<TicketType> ticketTypes, TicketAuditStatus status, String targetId, Pageable pageable);

    void reviewTicket(Long ticketId, TicketReviewRequest request);
}
