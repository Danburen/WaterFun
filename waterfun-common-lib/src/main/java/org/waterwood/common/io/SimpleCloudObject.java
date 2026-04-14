package org.waterwood.common.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCloudObject implements Serializable {
    private String key;
    private String type; // MIME Content-Type
    private Long size; // bytes

    public Map<String, Object> toMap() {
        return Map.of("key", key, "type", type, "size", size);
    }
}
