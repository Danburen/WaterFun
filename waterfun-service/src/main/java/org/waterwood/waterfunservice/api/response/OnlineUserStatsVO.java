package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlineUserStatsVO {
    private long onlineCount;
    private long todayNewUsers;
    private long todayPeakOnline;
}
