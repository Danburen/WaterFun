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
    private Long loginCount;
    private Long dailyPv;
    private Long newUsers;
    private Long newPosts;
    private Long peakOnline;
    private Instant updatedAt;
}
