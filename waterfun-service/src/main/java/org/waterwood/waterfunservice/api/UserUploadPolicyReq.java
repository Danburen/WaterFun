package org.waterwood.waterfunservice.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadPolicy;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUploadPolicyReq implements UploadPolicy {
    @NotNull
    private UserBizType bizType;
    private String bizId;
    @NotEmpty
    @Size(min = 1, max = 9)
    private List<String> exts;
}
