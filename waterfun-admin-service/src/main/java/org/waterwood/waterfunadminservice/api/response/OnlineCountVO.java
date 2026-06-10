package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlineCountVO {
    private long onlineCount;
    private long adminOnlineCount;
    private long pealOnlineCount;
}
