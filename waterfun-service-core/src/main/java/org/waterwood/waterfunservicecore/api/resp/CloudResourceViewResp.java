package org.waterwood.waterfunservicecore.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class CloudResourceViewResp implements Serializable {
    private String previewUrl;
}
