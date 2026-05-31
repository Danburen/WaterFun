package org.waterwood.common.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.utils.JsonUtil;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMeta {
    private String eTag;

    public String toJson(){
        return JsonUtil.toJson(this);
    }
}
