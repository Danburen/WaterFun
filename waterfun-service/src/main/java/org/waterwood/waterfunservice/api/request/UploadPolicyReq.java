package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservice.api.BizType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadPolicyReq {
    @NotNull
    private BizType bizType;
    private String bizId;
    @NotEmpty
    @Size(min = 1, max = 9)
    private List<String> exts;
}
