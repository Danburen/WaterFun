package org.waterwood.waterfunservice.api.response.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;

import java.util.List;

/**
 * Lightweight response for "who liked this post" — returns only a preview
 * (first N users) plus a total count, instead of the full user list.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikedUsersResp {
    private long totalCount;
    private List<UserBrief> previewUsers;
}
