package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SiteStatisticResponse {
    private LocalDate statDate;
    private Long dailyVisits;
    private Long dailyUv;
    private Long newUsers;
    private Long activeUsers;
    private Long peakOnline;
    private Instant updatedAt;
}
