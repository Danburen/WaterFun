package org.waterwood.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class LoginUser implements Serializable {
    private Long userId;
    private String jti;
}
