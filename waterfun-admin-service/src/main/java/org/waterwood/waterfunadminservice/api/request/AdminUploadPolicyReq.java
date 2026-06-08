package org.waterwood.waterfunadminservice.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunadminservice.service.content.AdminBizType;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadPolicy;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUploadPolicyReq implements UploadPolicy {
    @NotNull
    private AdminBizType bizType;
    @NotEmpty
    private String bizId;
    @NotEmpty
    @Size(min = 1, max = 9)
    private List<String> exts;
}
