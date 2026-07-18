package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpAccessLogResponse {
    private Long id;
    private String ip;
    private String requestPath;
    private String requestMethod;
    private Long userUid;
    private Short httpStatus;
    private String country;
    private String province;
    private String city;
    private Instant createdAt;
}
