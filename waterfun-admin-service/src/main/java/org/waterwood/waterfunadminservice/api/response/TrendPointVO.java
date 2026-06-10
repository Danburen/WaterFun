package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendPointVO {
    private LocalDate date;
    private long dailyPv;
    private long newUsers;
    private long newPosts;
}
