package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardOverviewVO {
    private long onlineUserCount;
    private long totalUsers;
    private long totalPosts;
    private long todayNewUsers;
    private long todayNewPosts;
    private long todayPv;
    private long pendingModerations;
    private long peakOnline;
}
