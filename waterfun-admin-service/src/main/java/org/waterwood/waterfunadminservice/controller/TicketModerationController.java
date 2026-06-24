package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.request.ticket.TicketReviewRequest;
import org.waterwood.waterfunadminservice.api.response.ticket.TicketResponse;
import org.waterwood.waterfunadminservice.service.ticket.TicketModerationService;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class TicketModerationController {

    private final TicketModerationService ticketModerationService;

    @GetMapping
    public ApiResponse<Page<TicketResponse>> listTickets(
            @RequestParam(required = false) String ticketTypes,
            @RequestParam(required = false) TicketAuditStatus status,
            @RequestParam(required = false) String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TicketType> types = null;
        if (ticketTypes != null && !ticketTypes.isBlank()) {
            types = new ArrayList<>();
            for (String s : ticketTypes.split(",")) {
                types.add(TicketType.valueOf(s.trim()));
            }
        }
        return ApiResponse.success(
                ticketModerationService.listTickets(types, status, targetId, PageRequest.of(page, size))
        );
    }

    @PostMapping("/{id}/review")
    public ApiResponse<Void> reviewTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketReviewRequest request) {
        ticketModerationService.reviewTicket(id, request);
        return ApiResponse.success();
    }
}
