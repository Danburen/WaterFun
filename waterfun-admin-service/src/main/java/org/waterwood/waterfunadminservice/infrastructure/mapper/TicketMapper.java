package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.response.ticket.TicketResponse;
import org.waterwood.waterfunservicecore.api.message.TicketMessage;
import org.waterwood.waterfunservicecore.entity.ticket.Ticket;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface TicketMapper {

    @Mapping(source = "submitter.uid", target = "submitterId")
    @Mapping(source = "id", target = "ticketId")
    TicketMessage toTicketMessage(Ticket ticket);

    @Mapping(source = "id", target = "ticketId")
    @Mapping(target = "submitter", ignore = true)
    @Mapping(target = "auditor", ignore = true)
    @Mapping(target = "evidenceResourceUuids", ignore = true)
    @Mapping(target = "originalPenalty", ignore = true)
    @Mapping(target = "timeline", ignore = true)
    TicketResponse toTicketResponse(Ticket ticket);
}
