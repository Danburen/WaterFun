package org.waterwood.waterfunadminservice.api.response.content.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;

@Data
@AllArgsConstructor
public class PostBrief {
    private Long postId;
    private String title;
    private String editedTitle;

    private UserBrief author;
}
