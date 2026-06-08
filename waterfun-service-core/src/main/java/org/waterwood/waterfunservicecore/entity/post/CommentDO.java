package org.waterwood.waterfunservicecore.entity.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDO {
    private Long authorUid;
    private String content;
    private Long postId;
}
