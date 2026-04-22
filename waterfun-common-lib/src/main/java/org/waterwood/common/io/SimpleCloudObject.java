package org.waterwood.common.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCloudObject implements Serializable {
    private String key;
    private FileMeta fileMeta;
}
