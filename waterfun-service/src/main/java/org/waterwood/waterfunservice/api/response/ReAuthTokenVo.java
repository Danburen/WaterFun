package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.common.TokenResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReAuthTokenVo {
    private TokenResult reAuthToken;
}
