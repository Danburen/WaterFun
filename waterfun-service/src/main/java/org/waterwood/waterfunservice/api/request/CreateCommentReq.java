package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentReq {
    @NotNull
    private Long postId;
    private Long parentId;
    @NotBlank
    private String content;
}
