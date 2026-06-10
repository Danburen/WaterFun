package org.waterwood.waterfunadminservice.api.response.monitor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.waterwood.waterfunservicecore.dto.JvmInfoVO;

@Getter
@AllArgsConstructor
public class MergedJvmInfo {
    private final JvmInfoVO admin;
    private final JvmInfoVO user;
    private final String userError;
}
