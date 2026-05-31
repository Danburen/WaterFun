package org.waterwood.common.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileProbeResult {
    private long size;
    private String mimeType;
    private FileMeta meta;
}
