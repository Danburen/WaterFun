package org.waterwood.waterfunservice.api;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentPreviewReq {
    @NotEmpty
    private String content;
}
