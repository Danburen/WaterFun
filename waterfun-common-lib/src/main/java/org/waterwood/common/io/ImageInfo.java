package org.waterwood.common.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo {
    private String key;
    private Integer height;
    private Integer width;
    private FileMeta meta;
}
