package org.waterwood.waterfunservice.api.response.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyPostsStatsResp {
    private long totalCount;
    private long publishedCount;
    private long draftCount;
    private long pendingCount;
    private long rejectedCount;
    private long totalLikeCount;
    private long followerCount;
}
