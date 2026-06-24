package org.waterwood.waterfunservice.api.response.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsResponse {
    private long reportCount;
    private long appealCount;
    private long feedbackCount;
    private long suggestionCount;
}
